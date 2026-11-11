package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.view.ICraftingLayoutView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 千年工艺 JEI GUI 界面绘制与分类定义。
 * 继承 {@link BaseGoalCraftingCategory}，并通过 {@link ICraftingLayoutView} 策略模式实现单分类多态聚合渲染。
 */
public class MillCraftingCategory extends BaseGoalCraftingCategory<IMillCraftingRecipe> {
    public static final String BUTTON_ID_VILLAGER_DETAIL = BaseGoalCraftingCategory.BUTTON_ID_VILLAGER_DETAIL;
    public static final String BUTTON_ID_BUILDING_DETAIL = BaseGoalCraftingCategory.BUTTON_ID_BUILDING_DETAIL;

    public MillCraftingCategory(IGuiHelper guiHelper, RecipeType<IMillCraftingRecipe> recipeType, Component title, ItemStack iconStack) {
        super(guiHelper, recipeType, title, iconStack);
    }
}
