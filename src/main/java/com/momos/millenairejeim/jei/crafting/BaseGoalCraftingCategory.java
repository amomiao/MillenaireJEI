package com.momos.millenairejeim.jei.crafting;

import com.mojang.blaze3d.platform.InputConstants;
import com.momos.millenairejeim.api.PlatformBridge;
import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.MillCraftingBaseRecipe;
import com.momos.millenairejeim.jei.crafting.view.CraftingViewRegistry;
import com.momos.millenairejeim.jei.crafting.view.ICraftingLayoutView;
import com.momos.millenairejeim.gui.MillPlusButtonWidget;
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
import net.minecraft.world.item.Items;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

import java.util.List;

/**
 * 千年工艺及采集类 JEI Category 通用基类。
 * <p>
 * 封装统一的 `160x110` 画布、多态视图槽位委托（{@link CraftingViewRegistry}）、
 * 工艺类型/执行村民/关联建筑三段摘要渲染，以及预留给 {@code MillenaireJEIe} 附属模组的 `+` 详情跳转按钮。
 * </p>
 */
public abstract class BaseGoalCraftingCategory<T extends IMillCraftingRecipe> extends AbstractRecipeCategory<T> {
    public static final String BUTTON_ID_VILLAGER_DETAIL = "villager_detail_plus";
    public static final String BUTTON_ID_BUILDING_DETAIL = "building_detail_plus";

    protected static final int VILLAGER_BUTTON_X = 142;
    protected static final int VILLAGER_BUTTON_Y = 74;

    protected static final int BUILDING_BUTTON_X = 142;
    protected static final int BUILDING_BUTTON_Y = 94;

    private final RecipeType<T> recipeType;
    private final Component title;
    private final IDrawable icon;

    private final IDrawable inputSlotDrawable;
    private final IDrawable outputSlotDrawable;
    private final IDrawableAnimated animatedArrow;

    private final MillPlusButtonWidget villagerPlusButton = new MillPlusButtonWidget(12, 12);
    private final MillPlusButtonWidget buildingPlusButton = new MillPlusButtonWidget(12, 12);

    protected BaseGoalCraftingCategory(IGuiHelper guiHelper, RecipeType<T> recipeType, Component title, ItemStack iconStack) {
        super(recipeType, title, guiHelper.createDrawableItemStack(safeIcon(iconStack)), 160, 110);
        ItemStack resolvedIcon = safeIcon(iconStack);
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(resolvedIcon);

        this.inputSlotDrawable = guiHelper.getSlotDrawable();
        this.outputSlotDrawable = guiHelper.getOutputSlot();
        this.animatedArrow = guiHelper.createAnimatedRecipeArrow(200);
    }

    private static ItemStack safeIcon(ItemStack iconStack) {
        return (iconStack != null && !iconStack.isEmpty()) ? iconStack : new ItemStack(Items.BARRIER);
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
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        ICraftingLayoutView view = CraftingViewRegistry.getView(recipe);
        view.buildSlots(builder, recipe, focuses);

        // 触发槽位注入事件（支持后置模组如 MillenaireJEIe 进行槽位监听）
        PlatformBridge.get().postRecipeSetSlot(builder, recipe, focuses);
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if (recipe == null) return;

        Font font = Minecraft.getInstance().font;

        ICraftingLayoutView view = CraftingViewRegistry.getView(recipe);
        view.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);

        String handlerId = recipe.getHandlerId() != null ? recipe.getHandlerId().toLowerCase() : "crafting";
        Component localizedCraftingType = MillenaireLocalizeHelper.getCraftingTypeName(handlerId);
        String rawGoalKey = recipe.getGoalKey() != null ? recipe.getGoalKey() : (recipe.getId() != null ? recipe.getId().getPath() : "unknown");
        Component localizedGoalName = (recipe instanceof MillCraftingBaseRecipe base)
                ? base.getDisplayGoalName()
                : MillenaireLocalizeHelper.getGoalName(rawGoalKey);
        String formattedCraftingType = localizedCraftingType.getString() + " - " + localizedGoalName.getString();

        String villagerSummary = buildVillagerSummary(recipe);
        String buildingSummary = buildBuildingSummary(recipe);

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

        guiGraphics.drawString(font, "§8" + labelCraftingType, 6, 45, 0x404040, false);
        guiGraphics.drawString(font, "§5" + formattedCraftingType, 12, 54, 0x404040, false);

        guiGraphics.drawString(font, "§8" + labelVillager, 6, 65, 0x404040, false);
        guiGraphics.drawString(font, "§2" + villagerSummary, 12, 74, 0x404040, false);

        guiGraphics.drawString(font, "§8" + labelBuilding, 6, 85, 0x404040, false);
        guiGraphics.drawString(font, "§3" + buildingSummary, 12, 94, 0x404040, false);

        boolean shouldRenderVillager = recipe.getVillagerTypes() != null && !recipe.getVillagerTypes().isEmpty()
                && PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_VILLAGER_DETAIL, false);
        if (shouldRenderVillager) {
            villagerPlusButton.render(guiGraphics, font, VILLAGER_BUTTON_X, VILLAGER_BUTTON_Y, mouseX, mouseY);
        }

        boolean shouldRenderBuilding = recipe.getAssociatedBuildingIds() != null && !recipe.getAssociatedBuildingIds().isEmpty()
                && PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_BUILDING_DETAIL, false);
        if (shouldRenderBuilding) {
            buildingPlusButton.render(guiGraphics, font, BUILDING_BUTTON_X, BUILDING_BUTTON_Y, mouseX, mouseY);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, T recipe, IFocusGroup focuses) {
        ICraftingLayoutView view = CraftingViewRegistry.getView(recipe);
        view.createRecipeExtras(builder, recipe, focuses);

        boolean shouldRenderVillager = recipe.getVillagerTypes() != null && !recipe.getVillagerTypes().isEmpty()
                && PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_VILLAGER_DETAIL, false);
        if (shouldRenderVillager) {
            builder.addInputHandler(createPlusButtonHandler(
                    VILLAGER_BUTTON_X, VILLAGER_BUTTON_Y, villagerPlusButton, recipe, BUTTON_ID_VILLAGER_DETAIL
            ));
        }

        boolean shouldRenderBuilding = recipe.getAssociatedBuildingIds() != null && !recipe.getAssociatedBuildingIds().isEmpty()
                && PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_BUILDING_DETAIL, false);
        if (shouldRenderBuilding) {
            builder.addInputHandler(createPlusButtonHandler(
                    BUILDING_BUTTON_X, BUILDING_BUTTON_Y, buildingPlusButton, recipe, BUTTON_ID_BUILDING_DETAIL
            ));
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (recipe == null) return;

        ICraftingLayoutView view = CraftingViewRegistry.getView(recipe);
        view.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);

        if (mouseX >= 6 && mouseX <= 154) {
            if (mouseY >= 65 && mouseY < 83) {
                List<VillagerType> villagers = recipe.getVillagerTypes();
                if (villagers != null && villagers.size() > 1) {
                    tooltip.add(Component.translatableWithFallback(
                            MillenaireJeimKeys.KEY_TOOLTIP_VILLAGERS_HEADER,
                            MillenaireJeimKeys.FALLBACK_TOOLTIP_VILLAGERS_HEADER
                    ));
                    villagers.forEach(v -> MillenaireJeimLocalizeHelper.addTooltipEntry(
                            tooltip, MillenaireJeimLocalizeHelper.getCraftingVillagerText(v)
                    ));
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
                    buildings.forEach(b -> MillenaireJeimLocalizeHelper.addTooltipEntry(
                            tooltip, MillenaireJeimLocalizeHelper.getCraftingBuildingText(b)
                    ));
                }
            }
        }
    }

    private String buildVillagerSummary(T recipe) {
        if (recipe instanceof MillCraftingBaseRecipe base && base.getCustomVillagerSummary() != null) {
            return base.getCustomVillagerSummary().getString();
        }
        List<VillagerType> villagers = recipe.getVillagerTypes();
        if (villagers == null || villagers.isEmpty()) {
            return Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_NONE_VILLAGER,
                    MillenaireJeimKeys.FALLBACK_NONE_VILLAGER
            ).getString();
        }
        String firstVillagerName = MillenaireJeimLocalizeHelper.getCraftingVillagerText(villagers.get(0)).getString();
        if (villagers.size() > 1) {
            String countSuffix = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_COUNT_PEOPLE,
                    MillenaireJeimKeys.FALLBACK_COUNT_PEOPLE,
                    villagers.size()
            ).getString();
            return firstVillagerName + " " + countSuffix;
        }
        return firstVillagerName;
    }

    private String buildBuildingSummary(T recipe) {
        if (recipe instanceof MillCraftingBaseRecipe base && base.getCustomBuildingSummary() != null) {
            return base.getCustomBuildingSummary().getString();
        }
        List<BuildingPlanSet> buildings = recipe.getAssociatedBuildingIds();
        if (buildings == null || buildings.isEmpty()) {
            return Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_NONE_BUILDING,
                    MillenaireJeimKeys.FALLBACK_NONE_BUILDING
            ).getString();
        }
        String firstBuildingName = MillenaireJeimLocalizeHelper.getCraftingBuildingText(buildings.get(0)).getString();
        if (buildings.size() > 1) {
            String countSuffix = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_COUNT_PLACES,
                    MillenaireJeimKeys.FALLBACK_COUNT_PLACES,
                    buildings.size()
            ).getString();
            return firstBuildingName + " " + countSuffix;
        }
        return firstBuildingName;
    }

    private IJeiInputHandler createPlusButtonHandler(int x, int y, MillPlusButtonWidget button, T recipe, String buttonId) {
        return new IJeiInputHandler() {
            @Override
            public ScreenRectangle getArea() {
                return new ScreenRectangle(x, y, button.getWidth(), button.getHeight());
            }

            @Override
            public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                InputConstants.Key key = input.getKey();
                if (key.getType() == InputConstants.Type.MOUSE && key.getValue() == 0) {
                    if (!input.isSimulate()) {
                        button.onClick();
                        PlatformBridge.get().postRecipeButtonClick(recipe, buttonId, mouseX, mouseY, 0);
                    }
                    return true;
                }
                return false;
            }
        };
    }
}
