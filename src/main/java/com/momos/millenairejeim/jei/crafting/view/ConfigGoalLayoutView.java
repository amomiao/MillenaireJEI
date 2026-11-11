package com.momos.millenairejeim.jei.crafting.view;

import com.momos.millenairejeim.jei.crafting.type.GoalConfigRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.MillCraftingBaseRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * 纯外部配置型 Goal 的 JEI 布局视图策略。
 * <p>同时支持在左侧显示输入与催化剂（单行最多 3 格，超出自动扩展为两行最多 6 格），
 * 右侧复用 {@link HarvestLootLayoutView#renderOutputGrid} 渲染产物与附加 Tooltip。</p>
 */
public class ConfigGoalLayoutView implements ICraftingLayoutView {

    @Override
    public boolean supports(IMillCraftingRecipe recipe) {
        return recipe instanceof GoalConfigRecipe;
    }

    @Override
    public void buildSlots(IRecipeLayoutBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        List<IMillCraftingRecipe.IngredientWithCount> inputs = recipe.getInputs();
        List<ItemStack> catalysts = recipe.getCatalysts();
        int totalLeft = Math.min(inputs.size() + catalysts.size(), 6);
        boolean singleRow = totalLeft <= 3;

        int slotIndex = 0;
        for (int i = 0; i < inputs.size() && slotIndex < 6; i++, slotIndex++) {
            int x = 6 + (slotIndex % 3) * 18;
            int y = singleRow ? 15 : (4 + (slotIndex / 3) * 18);
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

        for (int i = 0; i < catalysts.size() && slotIndex < 6; i++, slotIndex++) {
            int x = 6 + (slotIndex % 3) * 18;
            int y = singleRow ? 15 : (4 + (slotIndex / 3) * 18);
            builder.addSlot(RecipeIngredientRole.CATALYST, x, y)
                    .setStandardSlotBackground()
                    .addItemStack(catalysts.get(i));
        }

        HarvestLootLayoutView.renderOutputGrid(builder, recipe, recipe.getOutputs(), false);
    }

    @Override
    public void draw(IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if (recipe instanceof MillCraftingBaseRecipe base && base.getExtraInfoLine() != null) {
            Font font = Minecraft.getInstance().font;
            guiGraphics.drawString(font, base.getExtraInfoLine(), 6, 36, 0x555555, false);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(72, 15);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 6 && mouseX <= 150 && mouseY >= 34 && mouseY <= 44) {
            if (recipe instanceof MillCraftingBaseRecipe base && base.getExtraInfoTooltip() != null) {
                tooltip.addAll(base.getExtraInfoTooltip());
            }
        }
    }
}
