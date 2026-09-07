// region 千年工艺 JEI GUI 界面绘制与分类定义
package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.MillenaireJeiKeys;
import com.momos.millenairejeim.util.MillenaireJeimLocalizeHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
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
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

import java.util.Arrays;
import java.util.List;

/**
 * 千年工艺 JEI GUI 界面绘制与槽位布局类。
 * 关联 {@link MillCraftingRecipe} 完成在 JEI 窗口中的显示。
 * <p>
 * 已包含工艺类型（Crafting Type）、所属文化、制作村民及关联建筑等信息的完整展示。
 */
public class MillCraftingCategory implements IRecipeCategory<MillCraftingRecipe> {
    private final RecipeType<MillCraftingRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;

    // GUI 元素
    private final IDrawable inputSlotDrawable;
    private final IDrawable outputSlotDrawable;
    private final IDrawableAnimated animatedArrow;

    public MillCraftingCategory(IGuiHelper guiHelper, RecipeType<MillCraftingRecipe> recipeType, Component title, ItemStack iconStack) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);

        this.inputSlotDrawable = guiHelper.getSlotDrawable();
        this.outputSlotDrawable = guiHelper.getOutputSlot();
        this.animatedArrow = guiHelper.createAnimatedRecipeArrow(200);
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
        return 80;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MillCraftingRecipe recipe, IFocusGroup focuses) {
        List<MillCraftingRecipe.IngredientWithCount> inputs = recipe.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            int x = 6 + (i % 3) * 18;
            int y = 6 + (i / 3) * 18;
            MillCraftingRecipe.IngredientWithCount input = inputs.get(i);

            ItemStack[] displayStacks = Arrays.stream(input.ingredient().getItems())
                    .map(stack -> {
                        ItemStack copy = stack.copy();
                        copy.setCount(input.count());
                        return copy;
                    })
                    .toArray(ItemStack[]::new);

            builder.addSlot(RecipeIngredientRole.INPUT, x + 1, y + 1)
                    .addIngredients(VanillaTypes.ITEM_STACK, Arrays.asList(displayStacks));
        }

        List<ItemStack> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size() && i < 2; i++) {
            int x = 108;
            int y = (outputs.size() == 1) ? 15 : (6 + i * 18);

            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                    .addItemStack(outputs.get(i));
        }
    }

    @Override
    public void draw(MillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                inputSlotDrawable.draw(guiGraphics, 6 + col * 18, 6 + row * 18);
            }
        }

        animatedArrow.draw(guiGraphics, 72, 15);

        String rawCraftingType = recipe.getCraftingType();
        if (rawCraftingType == null || rawCraftingType.isEmpty()) {
            rawCraftingType = "crafting";
        }
        String craftingTypeKey = String.format(MillenaireJeiKeys.TEMPLATE_CRAFTING_TYPE, rawCraftingType.toLowerCase());
        Component localizedCraftingType = Component.translatableWithFallback(craftingTypeKey, rawCraftingType);
        String rawGoalKey = recipe.getGoalKey() != null ? recipe.getGoalKey() : (recipe.getId() != null ? recipe.getId().getPath() : "unknown");
        Component localizedGoalName = MillenaireLocalizeHelper.getGoalName(rawGoalKey);
        String formattedCraftingType = localizedCraftingType.getString() + " - " + localizedGoalName.getString();

        List<VillagerType> villagers = recipe.getVillagerTypes();
        List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();

        String villagerSummary;
        if (villagers.isEmpty()) {
            villagerSummary = Component.translatableWithFallback(
                    MillenaireJeiKeys.KEY_NONE_VILLAGER,
                    MillenaireJeiKeys.FALLBACK_NONE_VILLAGER
            ).getString();
        } else {
            String firstVillagerName = MillenaireJeimLocalizeHelper.getCraftingVillagerText(villagers.get(0)).getString();
            if (villagers.size() > 1) {
                String countSuffix = Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_COUNT_PEOPLE,
                        MillenaireJeiKeys.FALLBACK_COUNT_PEOPLE,
                        villagers.size()
                ).getString();
                villagerSummary = firstVillagerName + " " + countSuffix;
            } else {
                villagerSummary = firstVillagerName;
            }
        }

        String buildingSummary;
        if (buildings.isEmpty()) {
            buildingSummary = Component.translatableWithFallback(
                    MillenaireJeiKeys.KEY_NONE_BUILDING,
                    MillenaireJeiKeys.FALLBACK_NONE_BUILDING
            ).getString();
        } else {
            String firstBuildingName = MillenaireJeimLocalizeHelper.getCraftingBuildingText(buildings.get(0)).getString();
            if (buildings.size() > 1) {
                String countSuffix = Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_COUNT_PLACES,
                        MillenaireJeiKeys.FALLBACK_COUNT_PLACES,
                        buildings.size()
                ).getString();
                buildingSummary = firstBuildingName + " " + countSuffix;
            } else {
                buildingSummary = firstBuildingName;
            }
        }

        String labelCraftingType = Component.translatableWithFallback(
                MillenaireJeiKeys.KEY_LABEL_CRAFTING_TYPE,
                MillenaireJeiKeys.FALLBACK_LABEL_CRAFTING_TYPE
        ).getString();
        String labelVillager = Component.translatableWithFallback(
                MillenaireJeiKeys.KEY_LABEL_VILLAGER,
                MillenaireJeiKeys.FALLBACK_LABEL_VILLAGER
        ).getString();
        String labelBuilding = Component.translatableWithFallback(
                MillenaireJeiKeys.KEY_LABEL_BUILDING,
                MillenaireJeiKeys.FALLBACK_LABEL_BUILDING
        ).getString();

        guiGraphics.drawString(font, "§8" + labelCraftingType + "§5" + formattedCraftingType, 6, 44, 0x404040, false);
        guiGraphics.drawString(font, "§8" + labelVillager + "§2" + villagerSummary, 6, 55, 0x404040, false);
        guiGraphics.drawString(font, "§8" + labelBuilding + "§3" + buildingSummary, 6, 66, 0x404040, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, MillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 6 && mouseX <= 154) {
            // 2. 悬停在【制作村民】行 (y: 53 ~ 64)
            if (mouseY >= 53 && mouseY < 64) {
                List<VillagerType> villagers = recipe.getVillagerTypes();
                if (villagers.size() <= 1) {
                    return;
                }
                tooltip.add(Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_TOOLTIP_VILLAGERS_HEADER,
                        MillenaireJeiKeys.FALLBACK_TOOLTIP_VILLAGERS_HEADER
                ));
                if (villagers.isEmpty()) {
                    tooltip.add(Component.translatableWithFallback(
                            MillenaireJeiKeys.KEY_NONE_VILLAGER,
                            MillenaireJeiKeys.FALLBACK_NONE_VILLAGER
                    ));
                } else {
                    villagers.forEach(v -> {
                        Component vName = MillenaireJeimLocalizeHelper.getCraftingVillagerText(v);
                        // [新注释] 使用封装后的工具类方法追加列表项
                        MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, vName);
                    });
                }
                return;
            }

            // 3. 悬停在【关联建筑】行 (y: 64 ~ 75)
            if (mouseY >= 64 && mouseY <= 75) {
                List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();
                if (buildings.size() <= 1) {
                    return;
                }
                tooltip.add(Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_TOOLTIP_BUILDINGS_HEADER,
                        MillenaireJeiKeys.FALLBACK_TOOLTIP_BUILDINGS_HEADER
                ));
                if (buildings.isEmpty()) {
                    tooltip.add(Component.translatableWithFallback(
                            MillenaireJeiKeys.KEY_NONE_BUILDING,
                            MillenaireJeiKeys.FALLBACK_NONE_BUILDING
                    ));
                } else {
                    buildings.forEach(b -> {
                        Component bName = MillenaireJeimLocalizeHelper.getCraftingBuildingText(b);
                        // [新注释] 使用封装后的工具类方法追加列表项
                        MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, bName);
                    });
                }
                return;
            }
        }
    }
}
// endregion 千年工艺 JEI GUI 界面绘制与分类定义