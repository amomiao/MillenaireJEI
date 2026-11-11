package com.momos.millenairejeim.helper;

import com.momos.millenairejeim.config.MillJeiConfigManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.millenaire.Millenaire;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.gathering.GatheringGoal;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.ItemHelper;
import org.millenaire.item.ModItems;

import java.util.*;
import java.util.function.Function;

// region 千年 API 门面与助手封装
/**
 * 千年村庄 (Millenaire) 模组核心 API 统一封装辅助类。
 * 集中管理对千年前后端核心 API 的调用，并作为 {@link MillenaireDatabase} 的门面类对外提供高频查询。
 */
public final class MillenaireAPIHelper {
    private static Map<ResourceLocation, BuildingPlanSet> PLAN_TO_SET_CACHE = null;
    private MillenaireAPIHelper() {}

    /** 清空本地静态缓存（建议在重载数据包或离开世界时调用）*/
    public static void clearCache() {
        // [醒目新注释] 重置底层 MillenaireDatabase 数据库映射单例
        MillenaireDatabase.clear();
        SafeReflectionHelper.clearCache();
        MillJeiConfigManager.clearCache();
    }

    //Tag 基础标识与方法重载封装，自动规范小写路径并规避 Null 指针
    // 获得文化信息
    public static Map<ResourceLocation, Culture> getAllCultureType() { return ModCultures.getAllCultures(); }
    public static Set<ResourceLocation> getAllCultureIds() { return getAllCultureType().keySet(); }
    public static Collection<Culture> getAllCultures() { return getAllCultureType().values(); }
    public static Culture getCulture(ResourceLocation cultureId) { return cultureId == null ? null : ModCultures.getCulture(cultureId); }
    // tag
    public static final ResourceLocation TAG_CULTURE_BASE = ResourceLocation.fromNamespaceAndPath("millenaire", "culture");
    public static ResourceLocation getCultureTag(String culturePath) {
        if (culturePath == null || culturePath.isBlank()) return null;
        return ResourceLocation.fromNamespaceAndPath("millenaire", culturePath.toLowerCase(Locale.ROOT));
    }
    public static ResourceLocation getCultureTag(ResourceLocation cultureId) {return cultureId == null ? null : getCultureTag(cultureId.getPath());}
    public static ResourceLocation getCultureTag(Culture culture) {return culture == null ? null : getCultureTag(culture.id());}

    // 获得村庄信息
    public static Map<ResourceLocation, VillageType> getAllVillageType() { return ModCultures.getAllVillageTypes(); }
    public static Set<ResourceLocation> getAllVillageIds() { return getAllVillageType().keySet(); }
    public static Collection<VillageType> getAllVillages() { return getAllVillageType().values(); }
    public static VillageType getVillage(ResourceLocation villageId) { return villageId == null ? null : ModCultures.getVillageType(villageId); }
    // tag
    public static final ResourceLocation TAG_VILLAGE_BASE = ResourceLocation.fromNamespaceAndPath("millenaire", "village");
    public static ResourceLocation getVillageTag(String villagePath) {
        if (villagePath == null || villagePath.isBlank()) return null;
        return ResourceLocation.fromNamespaceAndPath("millenaire", villagePath.toLowerCase(Locale.ROOT));
    }
    public static ResourceLocation getVillageTag(ResourceLocation villageId) {return villageId == null ? null : getVillageTag(villageId.getPath());}
    public static ResourceLocation getVillageTag(VillageType village) {return village == null ? null : getVillageTag(village.id());}

    // 获得建筑信息
    public static Map<ResourceLocation, BuildingPlanSet> getAllBuildingPlanSets() { return ModCultures.getAllBuildingPlanSets(); }
    public static Set<ResourceLocation> getAllBuildingIds() { return getAllBuildingPlanSets().keySet(); }
    public static Collection<BuildingPlanSet> getAllBuildings() { return getAllBuildingPlanSets().values(); }
    // plan
    public static BuildingPlanSet getBuildingPlanSet(ResourceLocation buildingId) {
        if (buildingId == null) return null;
        Map<ResourceLocation, BuildingPlanSet> allSets = getAllBuildingPlanSets();
        if (allSets == null || allSets.isEmpty()) return null;
        BuildingPlanSet planSet = allSets.get(buildingId);
        if (planSet != null) return planSet;
        String path = buildingId.getPath();
        String namespace = buildingId.getNamespace();
        ResourceLocation fallbackRl = ResourceLocation.fromNamespaceAndPath("millenaire", namespace + "/" + path);
        planSet = allSets.get(fallbackRl);
        if (planSet != null) return planSet;
        for (Map.Entry<ResourceLocation, BuildingPlanSet> entry : allSets.entrySet()) {
            ResourceLocation key = entry.getKey();
            if (key.getPath().equals(path) || key.getPath().endsWith("/" + path)) {
                return entry.getValue();
            }
        }
        return null;
    }
    public static Map<ResourceLocation, BuildingPlan> getAllBuildingPlans() { return ModCultures.getAllBuildingPlans(); }

    // tag
    public static final ResourceLocation TAG_BUILDING_BASE = ResourceLocation.fromNamespaceAndPath("millenaire", "building");
    public static ResourceLocation getBuildingTag(String buildingId) {
        if (buildingId == null || buildingId.isBlank()) return null;
        return ResourceLocation.fromNamespaceAndPath("millenaire", buildingId.toLowerCase(Locale.ROOT));
    }
    public static ResourceLocation getBuildingTag(BuildingPlanSet building) {return building == null ? null : getBuildingTag(building.buildingId());}

    /**
     * 获取建筑的完整有效标签列表（含顶层 {@code tags}、所有变体/等级 {@link BuildingPlan#tags()} 以及子建筑授予的 {@code parentTags}）。
     */
    public static List<String> getBuildingAllTags(BuildingPlanSet building) {
        if (building == null) return List.of();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        if (db != null && building.id() != null) {
            return db.getBuildingAllTags(building.id());
        }
        return collectFallbackBuildingTags(building);
    }

    /**
     * 判断指定建筑是否拥有目标运行时标签（大小写不敏感，覆盖顶层/变体/等级/子建筑授予 parentTags）。
     */
    public static boolean buildingHasTag(BuildingPlanSet building, String tag) {
        if (building == null || tag == null || tag.isBlank()) return false;
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        if (db != null && building.id() != null) {
            return db.buildingHasTag(building.id(), tag);
        }
        String needle = tag.trim();
        for (String existing : collectFallbackBuildingTags(building)) {
            if (needle.equalsIgnoreCase(existing)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按建筑标签及可选附加 {@code requiredTag}、文化范围查询匹配的所有建筑计划集（按 ID 确定性排序）。
     */
    public static List<BuildingPlanSet> getBuildingsByTag(
            String buildingTag,
            String requiredTag,
            Collection<ResourceLocation> cultureFilter
    ) {
        if (buildingTag == null || buildingTag.isBlank()) {
            return List.of();
        }
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        Collection<BuildingPlanSet> candidates = db != null
                ? db.getBuildingsByTag(buildingTag)
                : getAllBuildings();
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        boolean hasCultureFilter = cultureFilter != null && !cultureFilter.isEmpty();
        boolean hasRequiredTag = requiredTag != null && !requiredTag.isBlank();
        List<BuildingPlanSet> result = new ArrayList<>();
        for (BuildingPlanSet bps : candidates) {
            if (bps == null) continue;
            if (hasCultureFilter && (bps.culture() == null || !cultureFilter.contains(bps.culture()))) {
                continue;
            }
            if (db == null && !buildingHasTag(bps, buildingTag)) {
                continue;
            }
            if (hasRequiredTag && !buildingHasTag(bps, requiredTag)) {
                continue;
            }
            result.add(bps);
        }
        result.sort(Comparator.comparing(b -> b.id() != null ? b.id().toString() : String.valueOf(b.buildingId())));
        return result;
    }

    /**
     * 获取指定父建筑直接关联的所有子建筑 {@link BuildingPlanSet} 列表（按 ID 排序）。
     */
    public static List<BuildingPlanSet> getBuildingSubBuildings(BuildingPlanSet building) {
        if (building == null || building.id() == null) return List.of();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getBuildingSubBuildings(building.id()) : List.of();
    }

    private static List<String> collectFallbackBuildingTags(BuildingPlanSet building) {
        Map<String, String> tagMap = new LinkedHashMap<>();
        if (building.tags() != null) {
            for (String t : building.tags()) {
                if (t != null && !t.isBlank()) {
                    String trimmed = t.trim();
                    tagMap.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
                }
            }
        }
        if (building.variants() != null) {
            for (List<BuildingPlanSet.LevelDef> levels : building.variants().values()) {
                if (levels == null) continue;
                for (BuildingPlanSet.LevelDef ld : levels) {
                    if (ld == null || ld.planId() == null) continue;
                    BuildingPlan plan = ModCultures.getBuildingPlan(ld.planId());
                    if (plan != null && plan.tags() != null) {
                        for (String t : plan.tags()) {
                            if (t != null && !t.isBlank()) {
                                String trimmed = t.trim();
                                tagMap.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
                            }
                        }
                    }
                }
            }
        }
        return tagMap.values().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    // 获得村民信息
    public static Map<ResourceLocation, VillagerType> getAllVillagerType() { return ModCultures.getAllVillagerTypes(); }
    public static Set<ResourceLocation> getAllVillagerIds() { return getAllVillagerType().keySet(); }
    public static Collection<VillagerType> getAllVillagers() { return getAllVillagerType().values(); }
    public static VillagerType getVillager(ResourceLocation villagerId) { return villagerId == null ? null : ModCultures.getVillagerType(villagerId); }
    // tag
    public static final ResourceLocation TAG_VILLAGER_BASE = ResourceLocation.fromNamespaceAndPath("millenaire", "villager");
    public static ResourceLocation getVillagerTag(String villagerPath) {
        if (villagerPath == null || villagerPath.isBlank()) return null;
        return ResourceLocation.fromNamespaceAndPath("millenaire", villagerPath.toLowerCase(Locale.ROOT));
    }
    public static ResourceLocation getVillagerTag(ResourceLocation villagerId) {return villagerId == null ? null : getVillagerTag(villagerId.getPath());}
    public static ResourceLocation getVillagerTag(VillagerType villager) {return villager == null ? null : getVillagerTag(villager.id());}

    // 获得 Goal 信息
    public static GoalRegistry getGoalRegistry() { return Millenaire.getGoalRegistry(); }
    public static List<ResourceLocation> getAllGoalIds() { return getGoalRegistry() == null ? Collections.emptyList() : getGoalRegistry().getAllIds(); }
    public static List<GatheringGoal> getAllGoals() { return getGoalRegistry() == null ? Collections.emptyList() : getGoalRegistry().getGatheringGoals(); }
    public static VillagerGoal getGoal(ResourceLocation goalId) { return getGoalRegistry() == null || goalId == null ? null : getGoalRegistry().get(goalId); }
    public static GatheringType getGatheringType(GatheringGoal goal) { return goal == null ? null : goal.getGatheringType(); }

    // 获得商店信息
    public static Map<String, ShopProfile> getShopProfiles(ResourceLocation cultureId) { return cultureId == null ? Collections.emptyMap() : ShopProfileLoader.getProfiles(cultureId); }
    public static TradeGood getTradeGood(ResourceLocation cultureId, String goodId) { return cultureId == null || goodId == null ? null : TradeGoodsLoader.getGoodById(cultureId, goodId); }
    public static Item resolveTradeGoodItem(TradeGood good) { return good == null ? null : good.resolveItem(); }
    public static BuildingPlanSet getBuildingPlanSetByPlan(BuildingPlan plan) {
        if (plan == null || plan.id() == null) {
            return null;
        }
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getBuildingSetByPlanId(plan.id()) : null;
    }

    // region 物品与货币系统 API
    public static Item resolveItem(String itemId) { return ItemHelper.resolve(itemId); }
    public static ItemStack getDenierOrStack(int count) { return new ItemStack(ModItems.DENIER_OR.get(), count); }
    public static ItemStack getDenierArgentStack(int count) { return new ItemStack(ModItems.DENIER_ARGENT.get(), count); }
    public static ItemStack getDenierStack(int count) { return new ItemStack(ModItems.DENIER.get(), count); }
    // endregion 物品与货币系统 API

    // region 文化与建筑村民关联过滤底层工具与门面 API
    public static <S, T> List<T> filterByResourceLocation(
            S source,
            Collection<T> targets,
            Function<S, ResourceLocation> sourceIdExtractor,
            Function<T, ResourceLocation> targetIdExtractor) {
        if (source == null || targets == null || targets.isEmpty() || sourceIdExtractor == null || targetIdExtractor == null) {
            return List.of();
        }
        ResourceLocation sourceId = sourceIdExtractor.apply(source);
        if (sourceId == null) {
            return List.of();
        }
        return targets.stream()
                .filter(Objects::nonNull)
                .filter(target -> sourceId.equals(targetIdExtractor.apply(target)))
                .toList();
    }

    public static <S, T> List<T> filterByResourceLocationMulti(
            S source,
            Collection<T> targets,
            Function<S, Collection<ResourceLocation>> sourceIdsExtractor,
            Function<T, Collection<ResourceLocation>> targetIdsExtractor) {
        if (source == null || targets == null || targets.isEmpty()
                || sourceIdsExtractor == null || targetIdsExtractor == null) {
            return List.of();
        }
        Collection<ResourceLocation> sourceIds = sourceIdsExtractor.apply(source);
        if (sourceIds == null || sourceIds.isEmpty()) {
            return List.of();
        }
        Set<ResourceLocation> sourceSet = new HashSet<>(sourceIds);
        return targets.stream()
                .filter(Objects::nonNull)
                .filter(target -> {
                    Collection<ResourceLocation> targetIds = targetIdsExtractor.apply(target);
                    return targetIds != null && targetIds.stream().anyMatch(sourceSet::contains);
                })
                .toList();
    }

    /** 抽取村庄配置中绑定的所有建筑 ID */
    public static Set<ResourceLocation> extractBuildingIdsFromVillage(VillageType village) {
        if (village == null) return Collections.emptySet();
        Set<ResourceLocation> ids = new HashSet<>();
        if (village.layout() != null) {
            for (VillageType.LayoutSlot slot : village.layout()) {
                if (slot != null && slot.plan() != null) {
                    ids.add(slot.plan());
                }
            }
        }
        if (village.playerBuildings() != null) {
            ids.addAll(village.playerBuildings());
        }
        return ids;
    }

    // [醒目新注释] 下述过滤 API 均转由底层 {@link MillenaireDatabase} 单例高效提供

    public static List<VillageType> getVillageFormCulture(Culture culture) {
        if (culture == null) return List.of();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getVillagesByCulture(culture.id()) : List.of();
    }

    public static List<BuildingPlanSet> getBuildingFormVillage(VillageType village) {
        if (village == null) return List.of();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getBuildingsByVillage(village.id()) : List.of();
    }

    public static List<VillagerType> getVillagerFormVillage(VillageType village) {
        if (village == null) return List.of();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getVillagersByVillage(village.id()) : List.of();
    }

    public static List<VillageType> getVillageFormBuilding(BuildingPlanSet building) {
        if (building == null) return List.of();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getVillagesByBuilding(building.id()) : List.of();
    }

    public static List<VillagerType> getVillagerFormBuilding(BuildingPlanSet building) {
        if (building == null) return List.of();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getVillagersByBuilding(building.id()) : List.of();
    }

    public static List<VillageType> getVillageFormVillager(VillagerType villager) {
        if (villager == null) return List.of();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getVillagesByVillager(villager.id()) : List.of();
    }

    public static List<BuildingPlanSet> getBuildingFormVillager(VillagerType villager) {
        if (villager == null) return List.of();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getBuildingsByVillager(villager.id()) : List.of();
    }

    public static List<BuildingPlanSet> MappingVillagerBuilding(ResourceLocation villagerTypeId) {
        if (villagerTypeId == null) return Collections.emptyList();
        MillenaireDatabase db = MillenaireDatabase.getInstance();
        return db != null ? db.getBuildingsByVillager(villagerTypeId) : Collections.emptyList();
    }

    public static List<BuildingPlanSet> MappingVillagerBuilding(VillagerType villagerType) {
        if (villagerType == null) return Collections.emptyList();
        return MappingVillagerBuilding(villagerType.id());
    }

    // endregion 文化与建筑村民关联过滤底层工具与门面 API

    // region 交易 API
    public static Set<BuildingPlan> getBuildingsByShopId(ResourceLocation cultureId, String shopId){
        Set<BuildingPlan> matchedBuildingKeys = new HashSet<>();
        if (cultureId == null || shopId == null) {
            return matchedBuildingKeys;
        }
        Map<ResourceLocation, BuildingPlan> buildingMap = getAllBuildingPlans();
        if (buildingMap != null) {
            for (BuildingPlan plan : buildingMap.values()) {
                if (plan != null && shopId.equals(plan.shopId()) && cultureId.equals(plan.culture())) {
                    matchedBuildingKeys.add(plan);
                }
            }
        }
        return matchedBuildingKeys;
    }

    /**
     * 根据文化 ID 和商店 ID 获取对应的建筑原名（Native Name）集合。
     * @param cultureId 文化 ID {@link ResourceLocation}
     * @param shopId    商店 ID {@link String}
     * @return 匹配到的建筑原名集合 {@link Set}
     */
    public static Set<String> getBuildingKeysByShopId(ResourceLocation cultureId, String shopId) {
        Set<String> matchedBuildingKeys = new HashSet<>();
        if (cultureId == null || shopId == null) {
            return matchedBuildingKeys;
        }
        for (BuildingPlan plan : getBuildingsByShopId(cultureId, shopId)) {
            if (plan != null) {
                BuildingPlanSet planSet = getBuildingPlanSetByPlan(plan);
                if (planSet != null && planSet.buildingId() != null) {
                    matchedBuildingKeys.add(planSet.buildingId());
                }
            }
        }
        return matchedBuildingKeys;
    }

    /**
     * 根据文化 ID 和商店 ID 获取对应的建筑 PlanSet 集合列表。
     */
    public static List<BuildingPlanSet> getBuildingPlanSetsByShopId(ResourceLocation cultureId, String shopId) {
        List<BuildingPlanSet> matchedPlanSets = new ArrayList<>();
        if (cultureId == null || shopId == null) {
            return matchedPlanSets;
        }
        for (BuildingPlan plan : getBuildingsByShopId(cultureId, shopId)) {
            if (plan != null) {
                BuildingPlanSet planSet = getBuildingPlanSetByPlan(plan);
                if (planSet != null && !matchedPlanSets.contains(planSet)) {
                    matchedPlanSets.add(planSet);
                }
            }
        }
        return matchedPlanSets;
    }
    // endregion 交易 API
}
// endregion 千年 API 门面与助手封装