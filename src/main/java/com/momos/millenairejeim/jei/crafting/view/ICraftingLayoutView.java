package com.momos.millenairejeim.jei.crafting.view;

import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 千年工艺单分类多态排版与绘制 View 策略接口。
 * 允许在单一 JEI 分类下优雅渲染合成、熔炼、收获、战利品及实体互动等多种变体。
 */
public interface ICraftingLayoutView {

    /**
     * 判断当前视图策略是否支持渲染该配方实体。
     */
    boolean supports(IMillCraftingRecipe recipe);

    /**
     * 构建 JEI 槽位布局。
     */
    void buildSlots(IRecipeLayoutBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses);

    /**
     * 绘制特异性背景切片、文字与状态。
     */
    void draw(IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY);

    /**
     * 声明式注入额外 UI 元素（如箭头、进度条等）。
     */
    default void createRecipeExtras(IRecipeExtrasBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {}

    /**
     * 收集特异性悬浮提示。
     */
    default void getTooltip(ITooltipBuilder tooltip, IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {}
}
