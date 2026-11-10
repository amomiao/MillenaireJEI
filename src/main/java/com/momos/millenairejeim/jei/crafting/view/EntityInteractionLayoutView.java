package com.momos.millenairejeim.jei.crafting.view;

import com.momos.millenairejeim.jei.crafting.type.GoalEntityInteractionRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
 * 实体互动（屠宰、繁殖、剪毛）配方视图策略。
 */
public class EntityInteractionLayoutView implements ICraftingLayoutView {

    @Override
    public boolean supports(IMillCraftingRecipe recipe) {
        return recipe instanceof GoalEntityInteractionRecipe;
    }

    @Override
    public void buildSlots(IRecipeLayoutBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        GoalEntityInteractionRecipe entityRecipe = (GoalEntityInteractionRecipe) recipe;

        // 输入工具/消耗品槽位
        List<IMillCraftingRecipe.IngredientWithCount> inputs = recipe.getInputs();
        if (inputs != null && !inputs.isEmpty()) {
            IMillCraftingRecipe.IngredientWithCount input = inputs.get(0);
            ItemStack[] displayStacks = Arrays.stream(input.ingredient().getItems())
                    .map(stack -> {
                        ItemStack copy = stack.copy();
                        copy.setCount(input.count());
                        return copy;
                    })
                    .toArray(ItemStack[]::new);

            builder.addInputSlot(16, 15)
                    .setStandardSlotBackground()
                    .addIngredients(VanillaTypes.ITEM_STACK, Arrays.asList(displayStacks));
        }

        // 目标实体生成蛋/催化剂槽位
        if (entityRecipe.hasSpawnEgg()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 42, 15)
                    .setStandardSlotBackground()
                    .addItemStack(entityRecipe.getEntitySpawnEgg())
                    .addRichTooltipCallback((slotView, tooltip) -> {
                        tooltip.add(Component.literal("§e目标生物: " + entityRecipe.getTargetEntityType()));
                    });
        }

        // 产出物槽位
        List<ItemStack> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size() && i < 2; i++) {
            int x = 108;
            int y = (outputs.size() == 1) ? 15 : (6 + i * 18);

            builder.addOutputSlot(x, y)
                    .setStandardSlotBackground()
                    .addItemStack(outputs.get(i));
        }
    }

    @Override
    public void draw(IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if (recipe instanceof GoalEntityInteractionRecipe entityRecipe) {
            Font font = Minecraft.getInstance().font;
            if (entityRecipe.getMinEntityCount() != null || entityRecipe.getMaxEntityCount() != null) {
                int min = entityRecipe.getMinEntityCount() != null ? entityRecipe.getMinEntityCount() : 0;
                int max = entityRecipe.getMaxEntityCount() != null ? entityRecipe.getMaxEntityCount() : 99;
                guiGraphics.drawString(font, "§7群体数量阈值: §e" + min + " - " + max, 6, 36, 0x555555, false);
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow()
                .setPosition(72, 15);
    }
}
