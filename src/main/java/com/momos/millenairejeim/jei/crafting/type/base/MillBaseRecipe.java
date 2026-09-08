package com.momos.millenairejeim.jei.crafting.type.base;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 千年工艺 JEI 配方通用基类。
 * 统一管理基础字段，并实现通用数据关联逻辑。
 */
public abstract class MillBaseRecipe implements IMillRecipe {

    private final ResourceLocation id;
    private final String goalKey;
    private final String handlerId;
    private final List<IngredientWithCount> inputs;
    private final List<ItemStack> outputs;
    private final List<ItemStack> catalysts;
    private final List<VillagerType> villagerTypes;

    public MillBaseRecipe(ResourceLocation id, String goalKey, String handlerId,
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
}