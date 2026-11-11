package com.momos.millenairejeim.jei.crafting.view;

import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.type.GoalChoppingRecipe;
import com.momos.millenairejeim.jei.crafting.type.GoalHarvestRecipe;
import com.momos.millenairejeim.jei.crafting.type.GoalLootRecipe;
import com.momos.millenairejeim.jei.crafting.type.GoalSpotRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.MillCraftingBaseRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 农耕收获、伐木采叶、定点采集与战利品/采掘多态视图策略。
 */
public class HarvestLootLayoutView implements ICraftingLayoutView {

    @Override
    public boolean supports(IMillCraftingRecipe recipe) {
        return recipe instanceof GoalHarvestRecipe
                || recipe instanceof GoalLootRecipe
                || recipe instanceof GoalChoppingRecipe
                || recipe instanceof GoalSpotRecipe;
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

            IRecipeSlotBuilder slot = builder.addInputSlot(x, y)
                    .setStandardSlotBackground()
                    .addIngredients(VanillaTypes.ITEM_STACK, Arrays.asList(displayStacks));
            if (recipe instanceof GoalChoppingRecipe) {
                slot.addRichTooltipCallback((slotView, tooltip) ->
                        tooltip.add(Component.translatableWithFallback(
                                MillenaireJeimKeys.KEY_TOOLTIP_TOOL_NOT_CONSUMED,
                                MillenaireJeimKeys.FALLBACK_TOOLTIP_TOOL_NOT_CONSUMED
                        ))
                );
            }
        }

        if (inputs.isEmpty() && !recipe.getCatalysts().isEmpty()) {
            List<ItemStack> catalysts = recipe.getCatalysts();
            for (int i = 0; i < catalysts.size() && i < 2; i++) {
                builder.addSlot(RecipeIngredientRole.CATALYST, 24 + i * 18, 15)
                        .setStandardSlotBackground()
                        .addItemStack(catalysts.get(i));
            }
        }

        if (recipe instanceof GoalChoppingRecipe chopping) {
            List<ItemStack> logs = prioritizeFocus(chopping.getLogOutputs(), focuses);
            List<ItemStack> saplings = prioritizeFocus(chopping.getSaplingOutputs(), focuses);

            IRecipeSlotBuilder logSlot = builder.addOutputSlot(108, 6)
                    .setStandardSlotBackground()
                    .addIngredients(VanillaTypes.ITEM_STACK, logs);
            attachOutputTooltips(logSlot, recipe);

            IRecipeSlotBuilder saplingSlot = builder.addOutputSlot(108, 24)
                    .setStandardSlotBackground()
                    .addIngredients(VanillaTypes.ITEM_STACK, saplings);
            attachOutputTooltips(saplingSlot, recipe);
            return;
        }

        boolean hasBonus = recipe instanceof GoalHarvestRecipe harvest && !harvest.getIrrigationBonusCrop().isEmpty();
        List<ItemStack> outputs = recipe.getOutputs();
        if (outputs.size() <= 2) {
            int baseX = hasBonus ? 102 : 108;
            for (int i = 0; i < outputs.size(); i++) {
                int y = (outputs.size() == 1) ? 15 : (6 + i * 18);
                IRecipeSlotBuilder slot = builder.addOutputSlot(baseX, y)
                        .setStandardSlotBackground()
                        .addItemStack(outputs.get(i));
                attachOutputTooltips(slot, recipe);
            }
        } else {
            int cols = hasBonus ? 2 : 3;
            int maxSlots = cols * 2;
            for (int i = 0; i < Math.min(outputs.size(), maxSlots); i++) {
                int x = 96 + (i % cols) * 18;
                int y = 4 + (i / cols) * 18;
                IRecipeSlotBuilder slot = builder.addOutputSlot(x, y).setStandardSlotBackground();
                if (i == maxSlots - 1 && outputs.size() > maxSlots) {
                    slot.addIngredients(VanillaTypes.ITEM_STACK, outputs.subList(maxSlots - 1, outputs.size()));
                } else {
                    slot.addItemStack(outputs.get(i));
                }
                attachOutputTooltips(slot, recipe);
            }
        }

        if (hasBonus) {
            GoalHarvestRecipe harvest = (GoalHarvestRecipe) recipe;
            int bonusX = outputs.size() >= 3 ? 134 : 128;
            builder.addSlot(RecipeIngredientRole.OUTPUT, bonusX, 15)
                    .setStandardSlotBackground()
                    .addItemStack(harvest.getIrrigationBonusCrop())
                    .addRichTooltipCallback((slotView, tooltip) ->
                            tooltip.add(Component.translatableWithFallback(
                                    MillenaireJeimKeys.KEY_TOOLTIP_IRRIGATION_BONUS,
                                    MillenaireJeimKeys.FALLBACK_TOOLTIP_IRRIGATION_BONUS
                            ))
                    );
        }
    }

    @Override
    public void draw(IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        if (recipe instanceof GoalHarvestRecipe harvest && harvest.getRipeAge() != null) {
            Component text = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_INFO_RIPE_STAGE,
                    MillenaireJeimKeys.FALLBACK_INFO_RIPE_STAGE,
                    harvest.getRipeAge()
            );
            guiGraphics.drawString(font, text, 6, 36, 0x555555, false);
        } else if (recipe instanceof MillCraftingBaseRecipe base && base.getExtraInfoLine() != null) {
            guiGraphics.drawString(font, base.getExtraInfoLine(), 6, 36, 0x555555, false);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow()
                .setPosition(72, 15);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 6 && mouseX <= 120 && mouseY >= 34 && mouseY <= 44) {
            if (recipe instanceof GoalHarvestRecipe harvest
                    && harvest.getTargetBlock() != null && !harvest.getTargetBlock().isEmpty()
                    && mouseX <= 60) {
                tooltip.add(Component.literal("§7目标方块: " + harvest.getTargetBlock()));
            }
            if (recipe instanceof MillCraftingBaseRecipe base && base.getExtraInfoTooltip() != null) {
                tooltip.addAll(base.getExtraInfoTooltip());
            }
        }
    }

    private static void attachOutputTooltips(IRecipeSlotBuilder slot, IMillCraftingRecipe recipe) {
        if (!(recipe instanceof MillCraftingBaseRecipe base)) {
            return;
        }
        slot.addRichTooltipCallback((slotView, tooltip) ->
                slotView.getDisplayedItemStack().ifPresent(displayed -> {
                    List<Component> lines = base.getOutputTooltip(displayed);
                    if (!lines.isEmpty()) {
                        tooltip.addAll(lines);
                    }
                })
        );
    }

    private static List<ItemStack> prioritizeFocus(List<ItemStack> stacks, IFocusGroup focuses) {
        if (stacks.isEmpty() || focuses == null) {
            return stacks;
        }
        List<ItemStack> focused = new ArrayList<>();
        List<ItemStack> others = new ArrayList<>();
        List<ItemStack> focusItems = focuses.getFocuses(VanillaTypes.ITEM_STACK)
                .map(IFocus::getTypedValue)
                .map(v -> v.getIngredient())
                .toList();
        for (ItemStack stack : stacks) {
            boolean matches = focusItems.stream().anyMatch(f -> ItemStack.isSameItemSameComponents(stack, f));
            if (matches) {
                focused.add(stack);
            } else {
                others.add(stack);
            }
        }
        if (focused.isEmpty()) {
            return stacks;
        }
        focused.addAll(others);
        return focused;
    }
}
