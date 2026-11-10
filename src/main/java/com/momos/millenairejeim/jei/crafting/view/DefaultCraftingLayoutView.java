package com.momos.millenairejeim.jei.crafting.view;

import com.momos.millenairejeim.jei.crafting.type.GoalCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * 标准工艺与熔炼配方布局视图。
 */
public class DefaultCraftingLayoutView implements ICraftingLayoutView {

    @Override
    public boolean supports(IMillCraftingRecipe recipe) {
        return recipe instanceof GoalCraftingRecipe || recipe != null;
    }

    @Override
    public void buildSlots(IRecipeLayoutBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        List<IMillCraftingRecipe.IngredientWithCount> inputs = recipe.getInputs();
        for (int i = 0; i < inputs.size() && i < 6; i++) {
            int x = 6 + (i % 3) * 18;
            int y = 6 + (i / 3) * 18;
            IMillCraftingRecipe.IngredientWithCount input = inputs.get(i);

            ItemStack[] displayStacks = Arrays.stream(input.ingredient().getItems())
                    .map(stack -> {
                        ItemStack copy = stack.copy();
                        copy.setCount(input.count());
                        return copy;
                    })
                    .toArray(ItemStack[]::new);

            builder.addInputSlot(x, y)
                    .setStandardSlotBackground()
                    .addIngredients(VanillaTypes.ITEM_STACK, Arrays.asList(displayStacks));
        }

        List<ItemStack> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size() && i < 2; i++) {
            int x = 108;
            int y = (outputs.size() == 1) ? 15 : (6 + i * 18);

            builder.addOutputSlot(x, y)
                    .setStandardSlotBackground()
                    .addItemStack(outputs.get(i));
        }

        List<ItemStack> catalysts = recipe.getCatalysts();
        if (catalysts != null && !catalysts.isEmpty()) {
            for (int i = 0; i < catalysts.size(); i++) {
                builder.addSlot(RecipeIngredientRole.CATALYST, 66 + (i * 18), 38)
                        .setStandardSlotBackground()
                        .addItemStack(catalysts.get(i));
            }
        }
    }

    @Override
    public void draw(IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 标准槽位背景与箭头已由 JEI 核心托管，无需手动绘制静态切图
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget()
                .setPosition(72, 15);
    }
}
