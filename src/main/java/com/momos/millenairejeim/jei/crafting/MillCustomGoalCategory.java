package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.type.GoalConfigRecipe;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Collectors;

/**
 * 纯外部配置驱动的千年配置目标 JEI Category（独立页签 {@code millenaire:custom_goals}）。
 * <p>完整加载 {@code config/millenairejeim/custom_goals/*.json} 中的 Goal 配置条目，
 * 并复用 {@link BaseGoalCraftingCategory} 的槽位布局及 {@code MillenaireJEIe} 村民/建筑加号跳转能力。</p>
 */
public class MillCustomGoalCategory extends BaseGoalCraftingCategory<GoalConfigRecipe> {

    public MillCustomGoalCategory(IGuiHelper guiHelper, RecipeType<GoalConfigRecipe> recipeType, Component title, ItemStack iconStack) {
        super(guiHelper, recipeType, title, iconStack);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, GoalConfigRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        super.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);
        if (recipe == null || mouseX < 6 || mouseX > 154) {
            return;
        }

        if (mouseY >= 45 && mouseY < 63) {
            if (!recipe.getSourceConfigFile().isEmpty()) {
                tooltip.add(Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_TOOLTIP_CONFIG_SOURCE_FILE,
                        MillenaireJeimKeys.FALLBACK_TOOLTIP_CONFIG_SOURCE_FILE,
                        recipe.getSourceConfigFile()
                ));
            }
            if (!recipe.getBoundGoalIds().isEmpty()) {
                String goalsText = recipe.getBoundGoalIds().stream()
                        .map(ResourceLocation::toString)
                        .collect(Collectors.joining(", "));
                tooltip.add(Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_TOOLTIP_CONFIG_BOUND_GOALS,
                        MillenaireJeimKeys.FALLBACK_TOOLTIP_CONFIG_BOUND_GOALS,
                        goalsText
                ));
            }
        } else if (mouseY >= 85 && mouseY <= 103 && !recipe.getBuildingTags().isEmpty()) {
            tooltip.add(Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TOOLTIP_SPOT_BUILDING_TAG,
                    MillenaireJeimKeys.FALLBACK_TOOLTIP_SPOT_BUILDING_TAG,
                    String.join(", ", recipe.getBuildingTags())
            ));
        }
    }
}
