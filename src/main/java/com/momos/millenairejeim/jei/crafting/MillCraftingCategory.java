package com.momos.millenairejeim.jei.crafting;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * 千年工艺 JEI GUI 界面绘制与槽位布局类。
 * 关联 {@link MillCraftingRecipe} 完成在 JEI 窗口中的显示。
 */
public class MillCraftingCategory implements IRecipeCategory<MillCraftingRecipe> {
    private final RecipeType<MillCraftingRecipe> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    public MillCraftingCategory(IGuiHelper guiHelper, RecipeType<MillCraftingRecipe> recipeType, Component title, ItemStack iconStack) {
        this.recipeType = recipeType;
        this.title = title;
        // 放置槽位与箭头的画布背景 (150 x 60 像素)
        this.background = guiHelper.createBlankDrawable(150, 60);
        this.icon = guiHelper.createDrawableItemStack(iconStack);
    }

    @Override
    public RecipeType<MillCraftingRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, MillCraftingRecipe recipe, IFocusGroup focuses) {
        // 绘制输入物品槽位（左侧 3x2 网格）
        List<MillCraftingRecipe.IngredientWithCount> inputs = recipe.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            int x = 12 + (i % 3) * 18;
            int y = 12 + (i / 3) * 18;

            MillCraftingRecipe.IngredientWithCount input = inputs.get(i);
            // 复制 Ingredient 匹配的堆叠并注入所需数量
            ItemStack[] displayStacks = Arrays.stream(input.ingredient().getItems())
                    .map(stack -> {
                        ItemStack copy = stack.copy();
                        copy.setCount(input.count());
                        return copy;
                    })
                    .toArray(ItemStack[]::new);

            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addIngredients(VanillaTypes.ITEM_STACK, Arrays.asList(displayStacks));
        }

        // 绘制输出物品槽位（右侧 2x2 网格）
        List<ItemStack> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            int x = 96 + (i % 2) * 18;
            int y = 12 + (i / 2) * 18;

            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                    .addItemStack(outputs.get(i));
        }
    }
}