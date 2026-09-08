// region MillCraftingCategory.java
package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.MillenaireJeiKeys;
import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
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
 * 千年工艺 JEI GUI 界面绘制与分类定义。
 * 重新由 Category 统一负责背景、槽位、动画及文本和 Tooltip 的全量渲染。
 */
public class MillCraftingCategory implements IRecipeCategory<IMillRecipe> {
    private final RecipeType<IMillRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;

    // GUI 基础纹理组件
    private final IDrawable inputSlotDrawable;
    private final IDrawable outputSlotDrawable;
    private final IDrawableAnimated animatedArrow;

    public MillCraftingCategory(IGuiHelper guiHelper, RecipeType<IMillRecipe> recipeType, Component title, ItemStack iconStack) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);

        this.inputSlotDrawable = guiHelper.getSlotDrawable();
        this.outputSlotDrawable = guiHelper.getOutputSlot();
        this.animatedArrow = guiHelper.createAnimatedRecipeArrow(200);
    }

    public IDrawable getInputSlotDrawable() {
        return inputSlotDrawable;
    }

    public IDrawable getOutputSlotDrawable() {
        return outputSlotDrawable;
    }

    public IDrawableAnimated getAnimatedArrow() {
        return animatedArrow;
    }

    @Override
    public RecipeType<IMillRecipe> getRecipeType() {
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
        return 105;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IMillRecipe recipe, IFocusGroup focuses) {
        List<IMillRecipe.IngredientWithCount> inputs = recipe.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            int x = 6 + (i % 3) * 18;
            int y = 6 + (i / 3) * 18;
            IMillRecipe.IngredientWithCount input = inputs.get(i);

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

    /* =========================================================================================================
     * [新注释] 【全量 GUI 渲染职责收回】
     * 由 Category 直接调取 {@link IMillRecipe} 的数据，统一绘制槽位背景、动画箭头以及下方文本信息。
     * ========================================================================================================= */
    @Override
    public void draw(IMillRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if (recipe == null) return;

        Font font = Minecraft.getInstance().font;

        // 1. 绘制 6 个输入槽位背景 (3x2 布局)
        if (inputSlotDrawable != null) {
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 3; col++) {
                    inputSlotDrawable.draw(guiGraphics, 6 + col * 18, 6 + row * 18);
                }
            }
        }

        // 2. 绘制进度动画箭头
        if (animatedArrow != null) {
            animatedArrow.draw(guiGraphics, 72, 15);
        }

        // 3. 构建制作类型文本（通过 handlerId 判断类型，兼容多态显示）
        String handlerId = recipe.getHandlerId() != null ? recipe.getHandlerId().toLowerCase() : "crafting";
        String craftingTypeKey = String.format(MillenaireJeiKeys.TEMPLATE_CRAFTING_TYPE, handlerId);

        // 3. 构建制作类型文本（通过 handlerId 判断类型）
        // [新注释] 针对各种 Handler 类型提供回退默认显示名称
        /*
         * [醒目新注释] 替换原有硬编码中文 Switch 判断。
         * 改为直接调用 {@link MillenaireLocalizeHelper#getCraftingTypeName(String)} 获取规范的本地化 Component，
         * 完美兼顾多语言语言包加载与标准英文 Fallback。
         */
        Component localizedCraftingType = MillenaireLocalizeHelper.getCraftingTypeName(handlerId);
        String rawGoalKey = recipe.getGoalKey() != null ? recipe.getGoalKey() : (recipe.getId() != null ? recipe.getId().getPath() : "unknown");
        Component localizedGoalName = MillenaireLocalizeHelper.getGoalName(rawGoalKey);
        String formattedCraftingType = localizedCraftingType.getString() + " - " + localizedGoalName.getString();

        // 4. 构建制作村民汇总文本
        List<VillagerType> villagers = recipe.getVillagerTypes();
        String villagerSummary;
        if (villagers == null || villagers.isEmpty()) {
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

        // 5. 构建关联建筑汇总文本
        List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();
        String buildingSummary;
        if (buildings == null || buildings.isEmpty()) {
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

        // 6. 获取固定标签文本
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

        // 7. 渲染文本内容
        guiGraphics.drawString(font, "§8" + labelCraftingType, 6, 45, 0x404040, false);
        guiGraphics.drawString(font, "§5" + formattedCraftingType, 12, 54, 0x404040, false);

        guiGraphics.drawString(font, "§8" + labelVillager, 6, 65, 0x404040, false);
        guiGraphics.drawString(font, "§2" + villagerSummary, 12, 74, 0x404040, false);

        guiGraphics.drawString(font, "§8" + labelBuilding, 6, 85, 0x404040, false);
        guiGraphics.drawString(font, "§3" + buildingSummary, 12, 94, 0x404040, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, IMillRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (recipe == null) return;

        if (mouseX >= 6 && mouseX <= 154) {
            // 悬停在【制作村民】区域 (y: 65 ~ 83)
            if (mouseY >= 65 && mouseY < 83) {
                List<VillagerType> villagers = recipe.getVillagerTypes();
                if (villagers != null && villagers.size() > 1) {
                    tooltip.add(Component.translatableWithFallback(
                            MillenaireJeiKeys.KEY_TOOLTIP_VILLAGERS_HEADER,
                            MillenaireJeiKeys.FALLBACK_TOOLTIP_VILLAGERS_HEADER
                    ));
                    villagers.forEach(v -> {
                        Component vName = MillenaireJeimLocalizeHelper.getCraftingVillagerText(v);
                        MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, vName);
                    });
                }
                return;
            }

            // 悬停在【关联建筑】区域 (y: 85 ~ 103)
            if (mouseY >= 85 && mouseY <= 103) {
                List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();
                if (buildings != null && buildings.size() > 1) {
                    tooltip.add(Component.translatableWithFallback(
                            MillenaireJeiKeys.KEY_TOOLTIP_BUILDINGS_HEADER,
                            MillenaireJeiKeys.FALLBACK_TOOLTIP_BUILDINGS_HEADER
                    ));
                    buildings.forEach(b -> {
                        Component bName = MillenaireJeimLocalizeHelper.getCraftingBuildingText(b);
                        MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, bName);
                    });
                }
            }
        }
    }
}
// endregion MillCraftingCategory.java