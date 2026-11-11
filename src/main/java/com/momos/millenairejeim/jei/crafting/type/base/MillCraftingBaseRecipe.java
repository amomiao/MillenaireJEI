package com.momos.millenairejeim.jei.crafting.type.base;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.building.BuildingPlanSet;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.millenaire.culture.VillagerType;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        if (villagerTypes == null || villagerTypes.isEmpty()) {
            return Collections.emptyList();
        }
        List<BuildingPlanSet> buildings = new ArrayList<>();
        for (VillagerType villagerType : villagerTypes) {
            if (villagerType == null) continue;
            List<BuildingPlanSet> mappedBuildings = MillenaireAPIHelper.MappingVillagerBuilding(villagerType);
            if (mappedBuildings != null) {
                for (BuildingPlanSet bps : mappedBuildings) {
                    if (bps != null && !buildings.contains(bps)) {
                        buildings.add(bps);
                    }
                }
            }
        }
        return buildings;
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
}