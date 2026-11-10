package com.momos.millenairejeim.jei.paint;

import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 漆刷/油漆桶染色 Category 渲染器。
 * 继承 {@link AbstractRecipeCategory} 满足现代规范，负责渲染 {@link MillPaintRecipe} 的槽位与 UI 文本。
 */
public class MillPaintCategory extends AbstractRecipeCategory<MillPaintRecipe> {
    private final RecipeType<MillPaintRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;

    public MillPaintCategory(IGuiHelper guiHelper, RecipeType<MillPaintRecipe> recipeType, Component title, ItemStack iconStack) {
        super(recipeType, title, guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack), 160, 60);
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MillPaintRecipe recipe, IFocusGroup focuses) {
        // 输入槽 1：待染色的基础/异色方块（自动轮播其他颜色）
        builder.addInputSlot(18, 22)
                .setStandardSlotBackground()
                .addIngredients(recipe.getInputBlockIngredient());

        // 输入槽 2：对应颜色的油漆桶
        builder.addInputSlot(50, 22)
                .setStandardSlotBackground()
                .addItemStack(recipe.getPaintBucketStack());

        // 输出槽：染色后的目标方块
        builder.addOutputSlot(114, 22)
                .setStandardSlotBackground()
                .addItemStack(recipe.getResultStack());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MillPaintRecipe recipe, IFocusGroup focuses) {
        // 使用现代 JEI 标准动画箭头替代手动文本箭头
        builder.addRecipeArrowWidget().setPosition(78, 22);
    }

    @Override
    public void draw(MillPaintRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // 渲染方块分类名称（如：砖块、花纹砖、阶梯等）
        guiGraphics.drawString(font, recipe.getBlockTypeName(), 5, 5, 0x404040, false);

        // 渲染合成加号连接符
        guiGraphics.drawString(font, "+", 40, 26, 0x888888, false);

        // 渲染目标颜色文本
        Component targetColorText = Component.translatable("color.minecraft." + recipe.getTargetColor().getName());
        Component colorInfo = Component.translatableWithFallback(
                MillenaireJeimKeys.KEY_PAINT_TARGET_COLOR,
                MillenaireJeimKeys.FALLBACK_PAINT_TARGET_COLOR,
                targetColorText
        );
        guiGraphics.drawString(font, colorInfo, 5, 48, 0x666666, false);
    }
}
