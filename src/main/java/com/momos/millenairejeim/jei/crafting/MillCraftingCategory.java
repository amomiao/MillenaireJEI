package com.momos.millenairejeim.jei.crafting;

import com.mojang.blaze3d.platform.InputConstants;
import com.momos.millenairejeim.api.PlatformBridge;
import com.momos.millenairejeim.event.MillRecipeButtonClickEvent;
import com.momos.millenairejeim.event.MillRecipeButtonRenderEvent;
import com.momos.millenairejeim.gui.MillPlusButtonWidget;
import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.view.CraftingViewRegistry;
import com.momos.millenairejeim.jei.crafting.view.ICraftingLayoutView;
import com.momos.millenairejeim.util.MillenaireJeimLocalizeHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

import java.util.List;

/**
 * 千年工艺 JEI GUI 界面绘制与分类定义。
 * 继承 {@link AbstractRecipeCategory} 满足现代 JEI 规范，并通过 {@link ICraftingLayoutView} 策略模式实现单分类多态聚合渲染。
 */
public class MillCraftingCategory extends AbstractRecipeCategory<IMillCraftingRecipe> {
    public static final String BUTTON_ID_VILLAGER_DETAIL = "villager_detail_plus";
    public static final String BUTTON_ID_BUILDING_DETAIL = "building_detail_plus";

    private static final int VILLAGER_BUTTON_X = 142;
    private static final int VILLAGER_BUTTON_Y = 74;

    private static final int BUILDING_BUTTON_X = 142;
    private static final int BUILDING_BUTTON_Y = 94;

    private final RecipeType<IMillCraftingRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;

    // 保留原有公开字段 getter 满足生态调用兼容
    private final IDrawable inputSlotDrawable;
    private final IDrawable outputSlotDrawable;
    private final IDrawableAnimated animatedArrow;

    private final MillPlusButtonWidget villagerPlusButton = new MillPlusButtonWidget(12, 12);
    private final MillPlusButtonWidget buildingPlusButton = new MillPlusButtonWidget(12, 12);

    public MillCraftingCategory(IGuiHelper guiHelper, RecipeType<IMillCraftingRecipe> recipeType, Component title, ItemStack iconStack) {
        super(recipeType, title, guiHelper.createDrawableItemStack(iconStack), 160, 110);
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
    public void setRecipe(IRecipeLayoutBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        ICraftingLayoutView view = CraftingViewRegistry.getView(recipe);
        view.buildSlots(builder, recipe, focuses);

        // 触发槽位注入事件（支持后置模组如 MillenaireJEIe 进行槽位监听）
        PlatformBridge.get().postRecipeSetSlot(builder, recipe, focuses);
    }

    @Override
    public void draw(IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if (recipe == null) return;

        Font font = Minecraft.getInstance().font;

        // 1. 委托多态视图绘制槽位补充元素或特异性背景
        ICraftingLayoutView view = CraftingViewRegistry.getView(recipe);
        view.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);

        // 2. 构建制作类型文本（通过 handlerId 判断类型，兼容多态显示）
        String handlerId = recipe.getHandlerId() != null ? recipe.getHandlerId().toLowerCase() : "crafting";
        Component localizedCraftingType = MillenaireLocalizeHelper.getCraftingTypeName(handlerId);
        String rawGoalKey = recipe.getGoalKey() != null ? recipe.getGoalKey() : (recipe.getId() != null ? recipe.getId().getPath() : "unknown");
        Component localizedGoalName = MillenaireLocalizeHelper.getGoalName(rawGoalKey);
        String formattedCraftingType = localizedCraftingType.getString() + " - " + localizedGoalName.getString();

        // 3. 构建制作村民汇总文本
        List<VillagerType> villagers = recipe.getVillagerTypes();
        String villagerSummary;
        if (villagers == null || villagers.isEmpty()) {
            villagerSummary = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_NONE_VILLAGER,
                    MillenaireJeimKeys.FALLBACK_NONE_VILLAGER
            ).getString();
        } else {
            String firstVillagerName = MillenaireJeimLocalizeHelper.getCraftingVillagerText(villagers.get(0)).getString();
            if (villagers.size() > 1) {
                String countSuffix = Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_COUNT_PEOPLE,
                        MillenaireJeimKeys.FALLBACK_COUNT_PEOPLE,
                        villagers.size()
                ).getString();
                villagerSummary = firstVillagerName + " " + countSuffix;
            } else {
                villagerSummary = firstVillagerName;
            }
        }

        // 4. 构建关联建筑汇总文本
        List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();
        String buildingSummary;
        if (buildings == null || buildings.isEmpty()) {
            buildingSummary = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_NONE_BUILDING,
                    MillenaireJeimKeys.FALLBACK_NONE_BUILDING
            ).getString();
        } else {
            String firstBuildingName = MillenaireJeimLocalizeHelper.getCraftingBuildingText(buildings.get(0)).getString();
            if (buildings.size() > 1) {
                String countSuffix = Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_COUNT_PLACES,
                        MillenaireJeimKeys.FALLBACK_COUNT_PLACES,
                        buildings.size()
                ).getString();
                buildingSummary = firstBuildingName + " " + countSuffix;
            } else {
                buildingSummary = firstBuildingName;
            }
        }

        // 5. 获取固定标签文本
        String labelCraftingType = Component.translatableWithFallback(
                MillenaireJeimKeys.KEY_LABEL_CRAFTING_TYPE,
                MillenaireJeimKeys.FALLBACK_LABEL_CRAFTING_TYPE
        ).getString();
        String labelVillager = Component.translatableWithFallback(
                MillenaireJeimKeys.KEY_LABEL_VILLAGER,
                MillenaireJeimKeys.FALLBACK_LABEL_VILLAGER
        ).getString();
        String labelBuilding = Component.translatableWithFallback(
                MillenaireJeimKeys.KEY_LABEL_BUILDING,
                MillenaireJeimKeys.FALLBACK_LABEL_BUILDING
        ).getString();

        // 6. 渲染文本内容
        guiGraphics.drawString(font, "§8" + labelCraftingType, 6, 45, 0x404040, false);
        guiGraphics.drawString(font, "§5" + formattedCraftingType, 12, 54, 0x404040, false);

        guiGraphics.drawString(font, "§8" + labelVillager, 6, 65, 0x404040, false);
        guiGraphics.drawString(font, "§2" + villagerSummary, 12, 74, 0x404040, false);

        guiGraphics.drawString(font, "§8" + labelBuilding, 6, 85, 0x404040, false);
        guiGraphics.drawString(font, "§3" + buildingSummary, 12, 94, 0x404040, false);

        // 7. 触发事件询问是否绘制【村民详情查看 (+) 按钮】
        boolean defaultVillagerRender = villagers != null && !villagers.isEmpty();
        boolean shouldRenderVillager = PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_VILLAGER_DETAIL, defaultVillagerRender);
        if (shouldRenderVillager) {
            villagerPlusButton.render(guiGraphics, font, VILLAGER_BUTTON_X, VILLAGER_BUTTON_Y, mouseX, mouseY);
        }

        // 8. 触发事件询问是否绘制【建筑详情查看 (+) 按钮】
        boolean defaultBuildingRender = buildings != null && !buildings.isEmpty();
        boolean shouldRenderBuilding = PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_BUILDING_DETAIL, defaultBuildingRender);
        if (shouldRenderBuilding) {
            buildingPlusButton.render(guiGraphics, font, BUILDING_BUTTON_X, BUILDING_BUTTON_Y, mouseX, mouseY);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        // 1. 委托多态视图挂载箭头等内置控件
        ICraftingLayoutView view = CraftingViewRegistry.getView(recipe);
        view.createRecipeExtras(builder, recipe, focuses);

        // 2. 村民加号按钮交互响应挂载
        boolean defaultVillagerRender = recipe.getVillagerTypes() != null && !recipe.getVillagerTypes().isEmpty();
        boolean shouldRenderVillager = PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_VILLAGER_DETAIL, defaultVillagerRender);

        if (shouldRenderVillager) {
            builder.addInputHandler(new IJeiInputHandler() {
                @Override
                public ScreenRectangle getArea() {
                    return new ScreenRectangle(VILLAGER_BUTTON_X, VILLAGER_BUTTON_Y, villagerPlusButton.getWidth(), villagerPlusButton.getHeight());
                }

                @Override
                public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    InputConstants.Key key = input.getKey();
                    if (key.getType() == InputConstants.Type.MOUSE && key.getValue() == 0) { // 0 代表鼠标左键
                        villagerPlusButton.onClick();
                        PlatformBridge.get().postRecipeButtonClick(recipe, BUTTON_ID_VILLAGER_DETAIL, mouseX, mouseY, 0);
                        return true;
                    }
                    return false;
                }
            });
        }

        // 3. 建筑加号按钮交互响应挂载
        boolean defaultBuildingRender = recipe.getAssociatedBuildingIds() != null && !recipe.getAssociatedBuildingIds().isEmpty();
        boolean shouldRenderBuilding = PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_BUILDING_DETAIL, defaultBuildingRender);

        if (shouldRenderBuilding) {
            builder.addInputHandler(new IJeiInputHandler() {
                @Override
                public ScreenRectangle getArea() {
                    return new ScreenRectangle(BUILDING_BUTTON_X, BUILDING_BUTTON_Y, buildingPlusButton.getWidth(), buildingPlusButton.getHeight());
                }

                @Override
                public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    InputConstants.Key key = input.getKey();
                    if (key.getType() == InputConstants.Type.MOUSE && key.getValue() == 0) { // 0 代表鼠标左键
                        buildingPlusButton.onClick();
                        PlatformBridge.get().postRecipeButtonClick(recipe, BUTTON_ID_BUILDING_DETAIL, mouseX, mouseY, 0);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, IMillCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (recipe == null) return;

        // 1. 优先收集多态视图专属悬浮提示
        ICraftingLayoutView view = CraftingViewRegistry.getView(recipe);
        view.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);

        // 2. 通用村民与建筑详情列表 Tooltip 悬浮展现
        if (mouseX >= 6 && mouseX <= 154) {
            if (mouseY >= 65 && mouseY < 83) {
                List<VillagerType> villagers = recipe.getVillagerTypes();
                if (villagers != null && villagers.size() > 1) {
                    tooltip.add(Component.translatableWithFallback(
                            MillenaireJeimKeys.KEY_TOOLTIP_VILLAGERS_HEADER,
                            MillenaireJeimKeys.FALLBACK_TOOLTIP_VILLAGERS_HEADER
                    ));
                    villagers.forEach(v -> {
                        Component vName = MillenaireJeimLocalizeHelper.getCraftingVillagerText(v);
                        MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, vName);
                    });
                }
                return;
            }

            if (mouseY >= 85 && mouseY <= 103) {
                List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();
                if (buildings != null && buildings.size() > 1) {
                    tooltip.add(Component.translatableWithFallback(
                            MillenaireJeimKeys.KEY_TOOLTIP_BUILDINGS_HEADER,
                            MillenaireJeimKeys.FALLBACK_TOOLTIP_BUILDINGS_HEADER
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
