package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.type.GoalSpotRecipe;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 千年定点采集与处理 JEI Category（独立页签，使用蚕块 {@code millenaire:silk_worm} 作为页签图标）。
 * <p>
 * 专门承载基于 {@link org.millenaire.goal.impl.SpotGatheringGoal} 反射解析出的特殊点位采集与晾晒工艺
 * （蚕茧、蜗牛紫染料、泥砖收集、湿砖日光晒干、甘蔗收种等），并完整复用 {@link BaseGoalCraftingCategory}
 * 的联动槽位与 {@code MillenaireJEIe} 加号跳转能力。
 * </p>
 */
public class MillSpotGatheringCategory extends BaseGoalCraftingCategory<GoalSpotRecipe> {

    public MillSpotGatheringCategory(IGuiHelper guiHelper, RecipeType<GoalSpotRecipe> recipeType, Component title, ItemStack iconStack) {
        super(guiHelper, recipeType, title, iconStack);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, GoalSpotRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        super.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);
        if (recipe == null || mouseX < 6 || mouseX > 154) {
            return;
        }

        // 悬停在工艺类型行时展示反射得到的市政厅库存上限与建筑标签详情
        if (mouseY >= 45 && mouseY < 63) {
            if (recipe.getTownhallLimit() > 0) {
                tooltip.add(Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_TOOLTIP_SPOT_TOWNHALL_LIMIT,
                        MillenaireJeimKeys.FALLBACK_TOOLTIP_SPOT_TOWNHALL_LIMIT,
                        recipe.getTownhallLimit()
                ));
            }
            if (!recipe.getBuildingTag().isEmpty()) {
                tooltip.add(Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_TOOLTIP_SPOT_BUILDING_TAG,
                        MillenaireJeimKeys.FALLBACK_TOOLTIP_SPOT_BUILDING_TAG,
                        recipe.getBuildingTag()
                ));
            }
        } else if (mouseY >= 85 && mouseY <= 103 && !recipe.getBuildingTag().isEmpty()) {
            tooltip.add(Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TOOLTIP_SPOT_BUILDING_TAG,
                    MillenaireJeimKeys.FALLBACK_TOOLTIP_SPOT_BUILDING_TAG,
                    recipe.getBuildingTag()
            ));
        }
    }
}
