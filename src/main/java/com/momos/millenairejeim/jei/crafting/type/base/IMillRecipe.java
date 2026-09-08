// region IMillRecipe.java
package com.momos.millenairejeim.jei.crafting.type.base;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

import java.util.List;

/**
 * 千年 JEI 配方通用接口契约。
 */
public interface IMillRecipe {

    record IngredientWithCount(Ingredient ingredient, int count) {}

    ResourceLocation getId();

    String getGoalKey();

    String getHandlerId();

    List<IngredientWithCount> getInputs();

    List<ItemStack> getOutputs();

    List<ItemStack> getCatalysts();

    List<VillagerType> getVillagerTypes();

    /**
     * 获取制作当前配方关联的所有村民居住/工作建筑图纸集合。
     * @return 汇总去重后的建筑资源位置 {@link BuildingPlanSet} 列表
     */
    List<BuildingPlanSet> getAssociatedBuildingIds();
}
// endregion IMillRecipe.java