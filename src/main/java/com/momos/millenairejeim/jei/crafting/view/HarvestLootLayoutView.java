package com.momos.millenairejeim.jei.crafting.view;

import com.momos.millenairejeim.jei.crafting.type.GoalHarvestRecipe;
import com.momos.millenairejeim.jei.crafting.type.GoalLootRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * 农耕收获与资源采掘/战利品多态视图策略。
 */
public class HarvestLootLayoutView implements ICraftingLayoutView {

    @Override
    public boolean supports(IMillCraftingRecipe recipe) {
        return recipe instanceof GoalHarvestRecipe || recipe instanceof GoalLootRecipe;
    }

    @Override
    public void buildSlots(IRecipeLayoutBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        List<IMillCraftingRecipe.IngredientWithCount> inputs = recipe.getInputs();
        for (int i = 0; i < inputs.size() && i < 3; i++) {
            int x = 6 + i * 18;
            int y = 15;
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
            int y = (outputs.size() == 1 && !(recipe instanceof GoalHarvestRecipe harvest && !harvest.getIrrigationBonusCrop().isEmpty())) ? 15 : (6 + i * 18);

            builder.addOutputSlot(x, y)
                    .setStandardSlotBackground()
                    .addItemStack(outputs.get(i));
        }

        // 针对收获类配方的额外灌溉产物槽位
        if (recipe instanceof GoalHarvestRecipe harvest && !harvest.getIrrigationBonusCrop().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 15)
                    .setStandardSlotBackground()
                    .addItemStack(harvest.getIrrigationBonusCrop())
                    .addRichTooltipCallback((slotView, tooltip) -> {
                        tooltip.add(Component.literal("§b[灌溉奖励 / Irrigation Bonus]"));
                    });
        }
    }

    @Override
    public void draw(IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        if (recipe instanceof GoalHarvestRecipe harvest) {
            if (harvest.getRipeAge() != null) {
                guiGraphics.drawString(font, "§7成熟阶段: §e" + harvest.getRipeAge(), 6, 36, 0x555555, false);
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget()
                .setPosition(72, 15);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (recipe instanceof GoalHarvestRecipe harvest) {
            if (harvest.getTargetBlock() != null && !harvest.getTargetBlock().isEmpty()) {
                if (mouseX >= 6 && mouseX <= 60 && mouseY >= 34 && mouseY <= 44) {
                    tooltip.add(Component.literal("§7目标方块: " + harvest.getTargetBlock()));
                }
            }
        }
    }
}
