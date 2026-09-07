package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
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

    // [新注释] 移除“所属文化”行后，画布高度由 90px 缩减为 80px
    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MillCraftingRecipe recipe, IFocusGroup focuses) {
        // 1. 输入槽位（左侧 3x2 网格）
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

        // 2. 输出槽位
        // -----------------------------------------------------------------------------------
        // [新注释] 移除输出槽位背景框逻辑。当只有 1 个输出物品时，动态计算 y = 15，
        // 与箭头 (y = 15, 高度 17px) 达成完美的垂直中心对齐；多输出时恢复垂直间隔。
        // -----------------------------------------------------------------------------------
        List<ItemStack> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size() && i < 2; i++) {
            int x = 108; // 紧跟箭头右侧
            int y = (outputs.size() == 1) ? 15 : (6 + i * 18);

            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                    .addItemStack(outputs.get(i));
        }
    }

    @Override
    public void draw(MillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // --- A. 绘制槽位背景与动画箭头 ---
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                inputSlotDrawable.draw(guiGraphics, 6 + col * 18, 6 + row * 18);
            }
        }

        // [新注释] 已移除右侧 outputSlotDrawable.draw 输出底座格子的绘制，保留无框纯物品渲染
        animatedArrow.draw(guiGraphics, 72, 15);

        // --- B. 绘制底部信息文本区域 ---
        String rawCraftingType = recipe.getCraftingType();
        if (rawCraftingType == null || rawCraftingType.isEmpty()) {
            rawCraftingType = "crafting";
        }
        // [新注释] 使用 MillenaireJeiKeys.TEMPLATE_CRAFTING_TYPE 格式化获取对应的本地化 Key（如 "jei.millenaire.crafting_type.crafting"）
        String craftingTypeKey = String.format(MillenaireJeiKeys.TEMPLATE_CRAFTING_TYPE, rawCraftingType.toLowerCase());
        Component localizedCraftingType = Component.translatableWithFallback(craftingTypeKey, rawCraftingType);
        String rawGoalKey = recipe.getGoalKey() != null ? recipe.getGoalKey() : (recipe.getId() != null ? recipe.getId().getPath() : "unknown");
        Component localizedGoalName = MillenaireLocalizeHelper.getGoalName(rawGoalKey);
        // [新注释] 将原本硬编码的 rawCraftingType 替换为本地化后的 localizedCraftingType 文本
        String formattedCraftingType = localizedCraftingType.getString() + " - " + localizedGoalName.getString();

        List<VillagerType> villagers = recipe.getVillagerTypes();
        List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();

        // -----------------------------------------------------------------------------------
        // [新注释] 格式化展示文本：重构为使用 Component.translatableWithFallback(...) 提取本地化，
        // 消除硬编码“无村民”、“无建筑”、“等X人”、“等X处”的字符串。
        // -----------------------------------------------------------------------------------
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

        // 提取前缀本地化标签
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

        // -----------------------------------------------------------------------------------
        // [新注释] 移除“所属文化”行，下面两行整体向上平移。
        // 同时替换高亮荧光色为沉稳高对比度配色：工艺类型(暗紫 §5)、制作村民(暗绿 §2)、关联建筑(暗青 §3)。
        // -----------------------------------------------------------------------------------
        guiGraphics.drawString(font, "§8" + labelCraftingType + "§5" + formattedCraftingType, 6, 44, 0x404040, false);
        guiGraphics.drawString(font, "§8" + labelVillager + "§2" + villagerSummary, 6, 55, 0x404040, false);
        guiGraphics.drawString(font, "§8" + labelBuilding + "§3" + buildingSummary, 6, 66, 0x404040, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, MillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 6 && mouseX <= 154) {
            // 工艺行是条目是唯一的，所以不需要悬浮框

            // 2. 悬停在【制作村民】行 (y: 53 ~ 64)
            // [新注释] 向上移动判定区间，重构 Tooltip 使用 {@link MillenaireJeiKeys} 与 {@link Component#translatableWithFallback}
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
                        tooltip.add(Component.translatableWithFallback(
                                MillenaireJeiKeys.KEY_TOOLTIP_ITEM_ENTRY,
                                MillenaireJeiKeys.FALLBACK_TOOLTIP_ITEM_ENTRY,
                                vName
                        ));
                    });
                }
                return;
            }

            // 3. 悬停在【关联建筑】行 (y: 64 ~ 75)
            // [新注释] 重构 Tooltip 标题与细项为本地化 Component
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
                        tooltip.add(Component.translatableWithFallback(
                                MillenaireJeiKeys.KEY_TOOLTIP_ITEM_ENTRY,
                                MillenaireJeiKeys.FALLBACK_TOOLTIP_ITEM_ENTRY,
                                bName
                        ));
                    });
                }
                return;
            }
        }
    }
}