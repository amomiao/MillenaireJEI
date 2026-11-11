package com.momos.millenairejeim.helper;

import com.momos.millenairejeim.util.VillagerBuildingMapper;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;

import java.util.*;

// region 关系数据库实体与预索引管理器
/**
 * 千年村庄 (Millenaire) 关系数据库类。
 * 负责在内存中一次性预构建并保存【文化-村庄-建筑-村民-建筑标签/子建筑】的全向双向引用映射，
 * 解决高频 JEI 检索与 UI 交互时的重复计算性能损耗。
 */
public final class MillenaireDatabase {
    private static volatile MillenaireDatabase instance = null;
    // m数据
    private final Map<ResourceLocation, BuildingPlanSet> planToBuildingSetMap;
    private final Map<ResourceLocation, List<String>> buildingAllTagsMap;
    private final Map<ResourceLocation, Set<String>> buildingLowerTagsMap;
    private final Map<String, List<BuildingPlanSet>> tagToBuildingsMap;
    private final Map<ResourceLocation, List<BuildingPlanSet>> buildingSubBuildingsMap;
    // mm数据
    // 预索引关系映射表缓存
    private final Map<ResourceLocation, List<VillageType>> cultureToVillagesMap;
    private final Map<ResourceLocation, List<BuildingPlanSet>> villageToBuildingsMap;
    private final Map<ResourceLocation, List<VillagerType>> villageToVillagersMap;
    private final Map<ResourceLocation, List<VillageType>> buildingToVillagesMap;
    private final Map<ResourceLocation, List<VillagerType>> buildingToVillagersMap;
    private final Map<ResourceLocation, List<BuildingPlanSet>> villagerToBuildingsMap;
    private final Map<ResourceLocation, List<VillageType>> villagerToVillagesMap;

    /**
     * 私有构造函数。全量扫描 {@link ModCultures} 中的数据并建立索引。
     */
    private MillenaireDatabase() {
        // 临时初始化集合映射
        Map<ResourceLocation, List<VillageType>> c2v = new HashMap<>();
        Map<ResourceLocation, List<BuildingPlanSet>> v2b = new HashMap<>();
        Map<ResourceLocation, List<VillagerType>> v2v = new HashMap<>();
        Map<ResourceLocation, List<VillageType>> b2v = new HashMap<>();
        Map<ResourceLocation, List<VillagerType>> b2vgt = new HashMap<>();
        Map<ResourceLocation, List<BuildingPlanSet>> vgt2b = new HashMap<>();
        Map<ResourceLocation, List<VillageType>> vgt2v = new HashMap<>();

        // 1. 获取基础数据集
        Collection<Culture> allCultures = MillenaireAPIHelper.getAllCultures();
        Collection<VillageType> allVillages = MillenaireAPIHelper.getAllVillages();
        Collection<BuildingPlanSet> allBuildings = MillenaireAPIHelper.getAllBuildings();
        Collection<VillagerType> allVillagers = MillenaireAPIHelper.getAllVillagers();

        // m
        Map<ResourceLocation, BuildingPlanSet> p2bSet = new HashMap<>();
        Map<ResourceLocation, BuildingPlanSet> buildingById = new HashMap<>();
        Map<String, BuildingPlanSet> buildingByCultureAndKey = new HashMap<>();
        Map<String, List<BuildingPlanSet>> buildingsByRawKey = new HashMap<>();
        for (BuildingPlanSet planSet : allBuildings) {
            if (planSet == null) continue;
            if (planSet.id() != null) {
                buildingById.put(planSet.id(), planSet);
            }
            if (planSet.culture() != null && planSet.buildingId() != null) {
                buildingByCultureAndKey.put(
                        planSet.culture() + "|" + planSet.buildingId().toLowerCase(Locale.ROOT),
                        planSet
                );
            }
            if (planSet.buildingId() != null) {
                buildingsByRawKey
                        .computeIfAbsent(planSet.buildingId().toLowerCase(Locale.ROOT), k -> new ArrayList<>())
                        .add(planSet);
            }
            if (planSet.variants() == null) continue;
            for (List<BuildingPlanSet.LevelDef> levels : planSet.variants().values()) {
                if (levels == null) continue;
                for (BuildingPlanSet.LevelDef levelDef : levels) {
                    if (levelDef != null && levelDef.planId() != null) {
                        p2bSet.put(levelDef.planId(), planSet);
                    }
                }
            }
        }

        // m2: 构建子建筑关联及全量运行时标签索引（顶层 tags + 变体/等级 BuildingPlan.tags() + 子建筑授予 parentTags）
        Comparator<BuildingPlanSet> buildingIdComp = Comparator.comparing(
                b -> b.id() != null ? b.id().toString() : String.valueOf(b.buildingId())
        );
        Map<ResourceLocation, Set<BuildingPlanSet>> parentToSubs = new HashMap<>();
        Map<ResourceLocation, Set<BuildingPlanSet>> subToParents = new HashMap<>();
        for (BuildingPlanSet planSet : allBuildings) {
            if (planSet == null || planSet.id() == null) continue;
            if (planSet.startingSubBuildings() != null) {
                for (String rawSub : planSet.startingSubBuildings()) {
                    linkSubBuilding(planSet, rawSub, buildingById, buildingByCultureAndKey, buildingsByRawKey, parentToSubs, subToParents);
                }
            }
            if (planSet.variants() != null) {
                for (List<BuildingPlanSet.LevelDef> levels : planSet.variants().values()) {
                    if (levels == null) continue;
                    for (BuildingPlanSet.LevelDef levelDef : levels) {
                        if (levelDef == null || levelDef.subBuildings() == null) continue;
                        for (String rawSub : levelDef.subBuildings()) {
                            linkSubBuilding(planSet, rawSub, buildingById, buildingByCultureAndKey, buildingsByRawKey, parentToSubs, subToParents);
                        }
                    }
                }
            }
        }
        // Fallback: 仅在尚未通过父建筑声明关联子建筑时，采纳 child.parentBuilding() 反向引用
        for (BuildingPlanSet child : allBuildings) {
            if (child == null || child.id() == null || subToParents.containsKey(child.id())) continue;
            BuildingPlanSet.ParentRef parentRef = child.parentBuilding();
            if (parentRef != null && parentRef.id() != null && !parentRef.id().isBlank()) {
                BuildingPlanSet parent = resolveBuildingRef(
                        child, parentRef.id(), buildingById, buildingByCultureAndKey, buildingsByRawKey
                );
                if (parent != null && parent.id() != null && !parent.id().equals(child.id())) {
                    parentToSubs.computeIfAbsent(parent.id(), k -> new LinkedHashSet<>()).add(child);
                    subToParents.computeIfAbsent(child.id(), k -> new LinkedHashSet<>()).add(parent);
                }
            }
        }

        Map<ResourceLocation, List<String>> bAllTags = new HashMap<>();
        Map<ResourceLocation, Set<String>> bLowerTags = new HashMap<>();
        Map<String, List<BuildingPlanSet>> t2b = new HashMap<>();
        Map<ResourceLocation, List<BuildingPlanSet>> bSubBuildings = new HashMap<>();

        for (BuildingPlanSet planSet : allBuildings) {
            if (planSet == null || planSet.id() == null) continue;
            Map<String, String> lowerToCanonicalTag = new LinkedHashMap<>();
            addTagsToMap(lowerToCanonicalTag, planSet.tags());
            if (planSet.variants() != null) {
                for (List<BuildingPlanSet.LevelDef> levels : planSet.variants().values()) {
                    if (levels == null) continue;
                    for (BuildingPlanSet.LevelDef levelDef : levels) {
                        if (levelDef == null || levelDef.planId() == null) continue;
                        BuildingPlan plan = ModCultures.getBuildingPlan(levelDef.planId());
                        if (plan != null) {
                            addTagsToMap(lowerToCanonicalTag, plan.tags());
                        }
                    }
                }
            }
            Set<BuildingPlanSet> children = parentToSubs.getOrDefault(planSet.id(), Collections.emptySet());
            for (BuildingPlanSet sub : children) {
                if (sub == null || sub.variants() == null) continue;
                for (List<BuildingPlanSet.LevelDef> subLevels : sub.variants().values()) {
                    if (subLevels == null) continue;
                    for (BuildingPlanSet.LevelDef subLd : subLevels) {
                        if (subLd != null) {
                            addTagsToMap(lowerToCanonicalTag, subLd.parentTags());
                        }
                    }
                }
            }

            List<String> sortedTags = lowerToCanonicalTag.values().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            Set<String> lowerSet = Collections.unmodifiableSet(new HashSet<>(lowerToCanonicalTag.keySet()));
            bAllTags.put(planSet.id(), sortedTags);
            bLowerTags.put(planSet.id(), lowerSet);
            for (String lowerTag : lowerSet) {
                t2b.computeIfAbsent(lowerTag, k -> new ArrayList<>()).add(planSet);
            }

            List<BuildingPlanSet> sortedSubs = children.stream()
                    .filter(Objects::nonNull)
                    .sorted(buildingIdComp)
                    .toList();
            if (!sortedSubs.isEmpty()) {
                bSubBuildings.put(planSet.id(), sortedSubs);
            }
        }
        Map<String, List<BuildingPlanSet>> frozenT2b = new HashMap<>();
        for (Map.Entry<String, List<BuildingPlanSet>> entry : t2b.entrySet()) {
            frozenT2b.put(entry.getKey(), entry.getValue().stream().distinct().sorted(buildingIdComp).toList());
        }

        // mm
        // 2. 建立 [文化 -> 村庄] 映射
        for (Culture culture : allCultures) {
            if (culture == null || culture.id() == null) continue;
            List<VillageType> villages = MillenaireAPIHelper.filterByResourceLocation(
                    culture, allVillages, Culture::id, VillageType::culture
            );
            c2v.put(culture.id(), Collections.unmodifiableList(villages));
        }

        // 3. 建立 [村庄 -> 建筑] 映射
        for (VillageType village : allVillages) {
            if (village == null || village.id() == null) continue;
            List<BuildingPlanSet> buildings = MillenaireAPIHelper.filterByResourceLocationMulti(
                    village,
                    allBuildings,
                    MillenaireAPIHelper::extractBuildingIdsFromVillage,
                    building -> List.of(building.id())
            );
            v2b.put(village.id(), Collections.unmodifiableList(buildings));
        }

        // 4. 建立 [村民 -> 建筑] 基础映射 (利用 {@link VillagerBuildingMapper})
        Map<ResourceLocation, List<ResourceLocation>> rawVillagerBuildingMap = VillagerBuildingMapper.buildVillagerToBuildingMap();
        for (VillagerType villager : allVillagers) {
            if (villager == null || villager.id() == null) continue;
            List<ResourceLocation> buildingIds = rawVillagerBuildingMap.getOrDefault(villager.id(), Collections.emptyList());
            List<BuildingPlanSet> buildings = buildingIds.stream()
                    .map(MillenaireAPIHelper::getBuildingPlanSet)
                    .filter(Objects::nonNull)
                    .toList();
            vgt2b.put(villager.id(), Collections.unmodifiableList(buildings));
        }

        // 5. 反向与导出推导映射 [建筑 -> 村庄], [建筑 -> 村民], [村庄 -> 村民], [村民 -> 村庄]
        for (BuildingPlanSet building : allBuildings) {
            if (building == null || building.id() == null) continue;

            // [建筑 -> 村庄]
            List<VillageType> villages = MillenaireAPIHelper.filterByResourceLocationMulti(
                    building,
                    allVillages,
                    b -> List.of(b.id()),
                    MillenaireAPIHelper::extractBuildingIdsFromVillage
            );
            b2v.put(building.id(), Collections.unmodifiableList(villages));

            // [建筑 -> 村民] (反向查哪种村民能住此建筑)
            List<VillagerType> villagersInBuilding = allVillagers.stream()
                    .filter(Objects::nonNull)
                    .filter(vgt -> {
                        List<BuildingPlanSet> bList = vgt2b.get(vgt.id());
                        return bList != null && bList.stream().anyMatch(b -> b.id().equals(building.id()));
                    })
                    .toList();
            b2vgt.put(building.id(), Collections.unmodifiableList(villagersInBuilding));
        }

        for (VillageType village : allVillages) {
            if (village == null || village.id() == null) continue;
            // [村庄 -> 村民] (从村庄所包含的建筑推导居住的村民)
            List<BuildingPlanSet> buildingsInVillage = v2b.getOrDefault(village.id(), Collections.emptyList());
            List<VillagerType> villagersInVillage = buildingsInVillage.stream()
                    .flatMap(b -> b2vgt.getOrDefault(b.id(), Collections.emptyList()).stream())
                    .distinct()
                    .toList();
            v2v.put(village.id(), Collections.unmodifiableList(villagersInVillage));
        }

        for (VillagerType villager : allVillagers) {
            if (villager == null || villager.id() == null) continue;
            // [村民 -> 村庄] (从村民居住的建筑推导分布的村庄)
            List<BuildingPlanSet> villagerBuildings = vgt2b.getOrDefault(villager.id(), Collections.emptyList());
            List<VillageType> villagesWithVillager = villagerBuildings.stream()
                    .flatMap(b -> b2v.getOrDefault(b.id(), Collections.emptyList()).stream())
                    .distinct()
                    .toList();
            vgt2v.put(villager.id(), Collections.unmodifiableList(villagesWithVillager));
        }

        // 赋值只读缓存
        this.planToBuildingSetMap = Collections.unmodifiableMap(p2bSet);
        this.buildingAllTagsMap = Collections.unmodifiableMap(bAllTags);
        this.buildingLowerTagsMap = Collections.unmodifiableMap(bLowerTags);
        this.tagToBuildingsMap = Collections.unmodifiableMap(frozenT2b);
        this.buildingSubBuildingsMap = Collections.unmodifiableMap(bSubBuildings);
        // mm
        this.cultureToVillagesMap = Collections.unmodifiableMap(c2v);
        this.villageToBuildingsMap = Collections.unmodifiableMap(v2b);
        this.villageToVillagersMap = Collections.unmodifiableMap(v2v);
        this.buildingToVillagesMap = Collections.unmodifiableMap(b2v);
        this.buildingToVillagersMap = Collections.unmodifiableMap(b2vgt);
        this.villagerToBuildingsMap = Collections.unmodifiableMap(vgt2b);
        this.villagerToVillagesMap = Collections.unmodifiableMap(vgt2v);
    }

    private static void linkSubBuilding(
            BuildingPlanSet parent,
            String rawSubRef,
            Map<ResourceLocation, BuildingPlanSet> buildingById,
            Map<String, BuildingPlanSet> buildingByCultureAndKey,
            Map<String, List<BuildingPlanSet>> buildingsByRawKey,
            Map<ResourceLocation, Set<BuildingPlanSet>> parentToSubs,
            Map<ResourceLocation, Set<BuildingPlanSet>> subToParents
    ) {
        if (parent == null || parent.id() == null || rawSubRef == null || rawSubRef.isBlank()) {
            return;
        }
        BuildingPlanSet sub = resolveBuildingRef(parent, rawSubRef, buildingById, buildingByCultureAndKey, buildingsByRawKey);
        if (sub != null && sub.id() != null && !sub.id().equals(parent.id())) {
            parentToSubs.computeIfAbsent(parent.id(), k -> new LinkedHashSet<>()).add(sub);
            subToParents.computeIfAbsent(sub.id(), k -> new LinkedHashSet<>()).add(parent);
        }
    }

    private static BuildingPlanSet resolveBuildingRef(
            BuildingPlanSet owner,
            String rawRef,
            Map<ResourceLocation, BuildingPlanSet> buildingById,
            Map<String, BuildingPlanSet> buildingByCultureAndKey,
            Map<String, List<BuildingPlanSet>> buildingsByRawKey
    ) {
        if (rawRef == null || rawRef.isBlank()) {
            return null;
        }
        String trimmed = rawRef.trim();
        if (trimmed.indexOf(':') >= 0) {
            ResourceLocation parsed = ResourceLocation.tryParse(trimmed);
            if (parsed != null) {
                BuildingPlanSet exact = buildingById.get(parsed);
                if (exact != null) {
                    return exact;
                }
            }
        }
        if (owner != null && owner.culture() != null) {
            BuildingPlanSet sameCulture = buildingByCultureAndKey.get(
                    owner.culture() + "|" + trimmed.toLowerCase(Locale.ROOT)
            );
            if (sameCulture != null) {
                return sameCulture;
            }
            ResourceLocation scoped = ResourceLocation.tryBuild(
                    owner.culture().getNamespace(),
                    owner.culture().getPath() + "/" + trimmed
            );
            if (scoped != null && buildingById.containsKey(scoped)) {
                return buildingById.get(scoped);
            }
        }
        if (trimmed.indexOf('/') >= 0) {
            ResourceLocation fallback = ResourceLocation.tryBuild("millenaire", trimmed);
            if (fallback != null && buildingById.containsKey(fallback)) {
                return buildingById.get(fallback);
            }
        }
        List<BuildingPlanSet> byRawKey = buildingsByRawKey.get(trimmed.toLowerCase(Locale.ROOT));
        if (byRawKey != null && byRawKey.size() == 1) {
            return byRawKey.get(0);
        }
        return null;
    }

    private static void addTagsToMap(Map<String, String> lowerToCanonical, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) continue;
            String trimmed = tag.trim();
            lowerToCanonical.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }
    }

    /**
     * 获取数据库全局单例（双重检查锁懒加载）。
     * 若 Millenaire 核心数据尚未加载完毕，则暂时返回 null，不进行硬锁定。
     * @return {@link MillenaireDatabase} 关系数据库实例
     */
    public static MillenaireDatabase getInstance() {
        if (instance == null) {
            synchronized (MillenaireDatabase.class) {
                if (instance == null) {
                    Collection<Culture> allCultures = MillenaireAPIHelper.getAllCultures();
                    // [醒目新注释] 保证核心 ModCultures 解析完成后再构建全局数据库，防止在加载早期读到空数据
                    if (allCultures != null && !allCultures.isEmpty()) {
                        instance = new MillenaireDatabase();
                    }
                }
            }
        }
        return instance;
    }

    /** 清空全局数据库实例缓存（在重载或登出世界时触发） */
    public static void clear() {
        synchronized (MillenaireDatabase.class) {
            instance = null;
        }
    }

    // region Getter 关系查询 API
    public BuildingPlanSet getBuildingSetByPlanId(ResourceLocation planId) {
        return planId == null ? null : planToBuildingSetMap.get(planId);
    }

    public List<String> getBuildingAllTags(ResourceLocation buildingId) {
        return buildingId == null ? List.of() : buildingAllTagsMap.getOrDefault(buildingId, List.of());
    }

    public boolean buildingHasTag(ResourceLocation buildingId, String tag) {
        if (buildingId == null || tag == null || tag.isBlank()) {
            return false;
        }
        Set<String> tags = buildingLowerTagsMap.get(buildingId);
        return tags != null && tags.contains(tag.trim().toLowerCase(Locale.ROOT));
    }

    public List<BuildingPlanSet> getBuildingsByTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return List.of();
        }
        return tagToBuildingsMap.getOrDefault(tag.trim().toLowerCase(Locale.ROOT), List.of());
    }

    public List<BuildingPlanSet> getBuildingSubBuildings(ResourceLocation buildingId) {
        return buildingId == null ? List.of() : buildingSubBuildingsMap.getOrDefault(buildingId, List.of());
    }
    // mm
    public List<VillageType> getVillagesByCulture(ResourceLocation cultureId) {
        return cultureId == null ? List.of() : cultureToVillagesMap.getOrDefault(cultureId, List.of());
    }

    public List<BuildingPlanSet> getBuildingsByVillage(ResourceLocation villageId) {
        return villageId == null ? List.of() : villageToBuildingsMap.getOrDefault(villageId, List.of());
    }

    public List<VillagerType> getVillagersByVillage(ResourceLocation villageId) {
        return villageId == null ? List.of() : villageToVillagersMap.getOrDefault(villageId, List.of());
    }

    public List<VillageType> getVillagesByBuilding(ResourceLocation buildingId) {
        return buildingId == null ? List.of() : buildingToVillagesMap.getOrDefault(buildingId, List.of());
    }

    public List<VillagerType> getVillagersByBuilding(ResourceLocation buildingId) {
        return buildingId == null ? List.of() : buildingToVillagersMap.getOrDefault(buildingId, List.of());
    }

    public List<BuildingPlanSet> getBuildingsByVillager(ResourceLocation villagerId) {
        return villagerId == null ? List.of() : villagerToBuildingsMap.getOrDefault(villagerId, List.of());
    }

    public List<VillageType> getVillagesByVillager(ResourceLocation villagerId) {
        return villagerId == null ? List.of() : villagerToVillagesMap.getOrDefault(villagerId, List.of());
    }
    // endregion Getter 关系查询 API
}
// endregion 关系数据库实体与预索引管理器