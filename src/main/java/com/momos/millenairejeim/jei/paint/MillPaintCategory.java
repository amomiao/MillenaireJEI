package com.momos.millenairejeim.jei.paint;

import com.momos.millenairejeim.jei.MillenaireJeiKeys;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 漆刷/油漆桶染色 Category 渲染器。
 * 负责渲染 {@link MillPaintRecipe} 的槽位布局（原方块 + 油漆桶 -> 染色方块）与 UI 文本。
 */
public class MillPaintCategory implements IRecipeCategory<MillPaintRecipe> {
    private final RecipeType<MillPaintRecipe> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    public MillPaintCategory(IGuiHelper guiHelper, RecipeType<MillPaintRecipe> recipeType, Component title, ItemStack iconStack) {
        this.recipeType = recipeType;
        this.title = title;
        // 背景设为 160x60 空白画布
        this.background = guiHelper.createBlankDrawable(160, 60);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<MillPaintRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MillPaintRecipe recipe, IFocusGroup focuses) {
        // 输入槽 1：待染色的基础/异色方块（自动轮播其他颜色）
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 22)
                .addIngredients(recipe.getInputBlockIngredient());

        // 输入槽 2：对应颜色的油漆桶
        builder.addSlot(RecipeIngredientRole.INPUT, 50, 22)
                .addItemStack(recipe.getPaintBucketStack());

        // 输出槽：染色后的目标方块
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 22)
                .addItemStack(recipe.getResultStack());
    }

    @Override
    public void draw(MillPaintRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // 渲染方块分类名称（如：砖块、花纹砖、阶梯等）
        guiGraphics.drawString(font, recipe.getBlockTypeName(), 5, 5, 0x404040, false);

        // 渲染合成连接符
        guiGraphics.drawString(font, "+", 40, 26, 0x888888, false);
        guiGraphics.drawString(font, "->", 86, 26, 0x888888, false);

        // 渲染目标颜色文本
        Component targetColorText = Component.translatable("color.minecraft." + recipe.getTargetColor().getName());
        Component colorInfo = Component.translatableWithFallback(
                MillenaireJeiKeys.KEY_PAINT_TARGET_COLOR,
                MillenaireJeiKeys.FALLBACK_PAINT_TARGET_COLOR,
                targetColorText
        );
        guiGraphics.drawString(font, colorInfo, 5, 48, 0x666666, false);
    }
}