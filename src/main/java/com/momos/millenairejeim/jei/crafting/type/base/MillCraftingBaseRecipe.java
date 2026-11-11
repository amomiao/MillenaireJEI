package com.momos.millenairejeim.jei.crafting.type.base;

import com.google.gson.JsonObject;
import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 千年工艺 JEI 配方通用基类。
 * 统一管理基础字段，并实现通用数据关联逻辑。
 */
public abstract class MillCraftingBaseRecipe implements IMillCraftingRecipe {

    private final ResourceLocation id;
    private final String goalKey;
    private final String handlerId;
    private final List<IngredientWithCount> inputs;
    private final List<ItemStack> outputs;
    private final List<ItemStack> catalysts;
    private final List<VillagerType> villagerTypes;
    private final Map<Item, List<Component>> outputTooltips = new LinkedHashMap<>();
    @Nullable
    private String buildingTag;
    @Nullable
    private String requiredTag;
    @Nullable
    private Component customVillagerSummary;
    @Nullable
    private Component customBuildingSummary;
    @Nullable
    private Component extraInfoLine;
    @Nullable
    private List<Component> extraInfoTooltip;

    public MillCraftingBaseRecipe(ResourceLocation id, String goalKey, String handlerId,
                                  List<IngredientWithCount> inputs, List<ItemStack> outputs,
                                  List<ItemStack> catalysts, List<VillagerType> villagerTypes) {
        this.id = id;
        this.goalKey = goalKey;
        this.handlerId = handlerId;
        this.inputs = inputs != null ? inputs : Collections.emptyList();
        this.outputs = outputs != null ? outputs : Collections.emptyList();
        this.catalysts = catalysts != null ? catalysts : Collections.emptyList();
        this.villagerTypes = villagerTypes != null ? villagerTypes : Collections.emptyList();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public String getGoalKey() {
        return goalKey;
    }

    @Override
    public String getHandlerId() {
        return handlerId;
    }

    @Override
    @Nullable
    public String getBuildingTag() {
        return buildingTag;
    }

    @Override
    @Nullable
    public String getRequiredTag() {
        return requiredTag;
    }

    /** 设置当前配方在底层 Goal 执行时要求的目标建筑标签（{@code buildingTag}）与前置必需标签（{@code requiredTag}）。 */
    public void setTagConstraints(@Nullable String buildingTag, @Nullable String requiredTag) {
        this.buildingTag = normalizeTag(buildingTag);
        this.requiredTag = normalizeTag(requiredTag);
    }

    /** 从 {@link GatheringType#handlerParams()} 提取 {@code buildingTag} / {@code requiredTag} 约束。 */
    public void applyTagConstraints(@Nullable GatheringType type) {
        if (type == null || type.handlerParams() == null) {
            return;
        }
        JsonObject params = type.handlerParams();
        String bt = GsonHelper.getAsString(params, "buildingTag", null);
        String rt = GsonHelper.getAsString(params, "requiredTag", null);
        setTagConstraints(bt, rt);
    }

    @Override
    public List<IngredientWithCount> getInputs() {
        return inputs;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return outputs;
    }

    @Override
    public List<ItemStack> getCatalysts() {
        return catalysts;
    }

    @Override
    public List<VillagerType> getVillagerTypes() {
        return villagerTypes;
    }

    @Override
    public List<BuildingPlanSet> getAssociatedBuildingIds() {
        Comparator<BuildingPlanSet> byId = Comparator.comparing(
                b -> b.id() != null ? b.id().toString() : String.valueOf(b.buildingId())
        );
        Set<ResourceLocation> villagerCultures = new LinkedHashSet<>();
        Set<BuildingPlanSet> residingBuildings = new LinkedHashSet<>();
        if (villagerTypes != null) {
            for (VillagerType villagerType : villagerTypes) {
                if (villagerType == null) continue;
                if (villagerType.culture() != null) {
                    villagerCultures.add(villagerType.culture());
                }
                List<BuildingPlanSet> mappedBuildings = MillenaireAPIHelper.MappingVillagerBuilding(villagerType);
                if (mappedBuildings != null) {
                    for (BuildingPlanSet bps : mappedBuildings) {
                        if (bps != null) {
                            residingBuildings.add(bps);
                        }
                    }
                }
            }
        }

        // 1. 若指定了 buildingTag，严格对齐 AbstractGatheringHandler.resolveCandidateBuildings:
        // 在执行村民所属文化(无执行者时为全量建筑)中检索拥有该标签的全部运行建筑，并按 requiredTag 过滤
        if (buildingTag != null && !buildingTag.isBlank()) {
            return MillenaireAPIHelper.getBuildingsByTag(buildingTag, requiredTag, villagerCultures);
        }

        // 2. 若未指定 buildingTag 但指定了 requiredTag，则仅保留具备该标签的村民居住建筑（排除如 banditcamp 等无 forge 标签建筑）
        if (residingBuildings.isEmpty()) {
            return Collections.emptyList();
        }
        if (requiredTag != null && !requiredTag.isBlank()) {
            return residingBuildings.stream()
                    .filter(bps -> MillenaireAPIHelper.buildingHasTag(bps, requiredTag))
                    .sorted(byId)
                    .toList();
        }

        // 3. 无标签限制时返回确定性排序的居住建筑列表
        return residingBuildings.stream().sorted(byId).toList();
    }

    /** 为指定产出物附加概率、掉落范围或说明 Tooltip */
    public void addOutputTooltip(Item item, Component line) {
        if (item == null || line == null) return;
        outputTooltips.computeIfAbsent(item, k -> new ArrayList<>()).add(line);
    }

    /** 批量为指定产出物附加 Tooltip */
    public void addOutputTooltips(Item item, List<Component> lines) {
        if (item == null || lines == null || lines.isEmpty()) return;
        outputTooltips.computeIfAbsent(item, k -> new ArrayList<>()).addAll(lines);
    }

    /** 获取指定产出物叠堆对应的附加 Tooltip 列表 */
    public List<Component> getOutputTooltip(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Collections.emptyList();
        }
        List<Component> list = outputTooltips.get(stack.getItem());
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /** 获取前端展示用的目标名称组件（子类可覆写以提供外部配置自定义标题） */
    public Component getDisplayGoalName() {
        String rawGoalKey = goalKey != null ? goalKey : (id != null ? id.getPath() : "unknown");
        return MillenaireLocalizeHelper.getGoalName(rawGoalKey);
    }

    @Nullable
    public Component getCustomVillagerSummary() {
        return customVillagerSummary;
    }

    public void setCustomVillagerSummary(@Nullable Component customVillagerSummary) {
        this.customVillagerSummary = customVillagerSummary;
    }

    @Nullable
    public Component getCustomBuildingSummary() {
        return customBuildingSummary;
    }

    public void setCustomBuildingSummary(@Nullable Component customBuildingSummary) {
        this.customBuildingSummary = customBuildingSummary;
    }

    @Nullable
    public Component getExtraInfoLine() {
        return extraInfoLine;
    }

    @Nullable
    public List<Component> getExtraInfoTooltip() {
        return extraInfoTooltip;
    }

    public void setExtraInfo(@Nullable Component extraInfoLine, @Nullable List<Component> extraInfoTooltip) {
        this.extraInfoLine = extraInfoLine;
        this.extraInfoTooltip = extraInfoTooltip != null ? List.copyOf(extraInfoTooltip) : null;
    }

    protected static boolean containsItem(List<ItemStack> stacks, Item targetItem) {
        if (stacks == null || targetItem == null) return false;
        for (ItemStack s : stacks) {
            if (s != null && !s.isEmpty() && s.getItem() == targetItem) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static String normalizeTag(@Nullable String rawTag) {
        if (rawTag == null || rawTag.isBlank()) {
            return null;
        }
        return rawTag.trim();
    }
}
