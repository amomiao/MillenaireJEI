package com.momos.millenairejeim.jei.Trade;

import com.mojang.blaze3d.platform.InputConstants;
import com.momos.millenairejeim.api.PlatformBridge;
import com.momos.millenairejeim.gui.MillPlusButtonWidget;
import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.util.MillenaireJeimLocalizeHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlanSet;

import java.util.List;
import java.util.Set;

/**
 * 千年村庄交易 JEI 分类渲染器。
 * 继承 {@link AbstractRecipeCategory} 满足现代 JEI 标准规范。
 */
public class MillTradeCategory extends AbstractRecipeCategory<MillTradeRecipe> {
    public static final String BUTTON_ID_BUILDING_DETAIL = "trade_building_detail_plus";
    private static final int BUILDING_BUTTON_X = 142;
    private static final int BUILDING_BUTTON_Y = 5;

    private final RecipeType<MillTradeRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final boolean isSellingCategory;

    private final MillPlusButtonWidget buildingPlusButton = new MillPlusButtonWidget(12, 12);

    public MillTradeCategory(IGuiHelper guiHelper, RecipeType<MillTradeRecipe> recipeType, Component title, IDrawable icon, boolean isSellingCategory) {
        super(recipeType, title, icon, 160, 60);
        this.recipeType = recipeType;
        this.title = title;
        this.icon = icon;
        this.isSellingCategory = isSellingCategory;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MillTradeRecipe recipe, IFocusGroup focuses) {
        int coinCount = recipe.getCoinStacks().size();

        if (isSellingCategory) {
            // 玩家购入：输入货币，输出商品
            int coinStartX = 34 - (coinCount * 9);
            for (int i = 0; i < coinCount; i++) {
                builder.addInputSlot(coinStartX + (i * 18), 26)
                        .setStandardSlotBackground()
                        .addItemStack(recipe.getCoinStacks().get(i));
            }
            builder.addOutputSlot(117, 26)
                    .setStandardSlotBackground()
                    .addIngredients(recipe.getItemIngredient());
        } else {
            // 玩家售出：输入商品，输出货币
            builder.addInputSlot(25, 26)
                    .setStandardSlotBackground()
                    .addIngredients(recipe.getItemIngredient());

            int coinStartX = 126 - (coinCount * 9);
            for (int i = 0; i < coinCount; i++) {
                builder.addOutputSlot(coinStartX + (i * 18), 26)
                        .setStandardSlotBackground()
                        .addItemStack(recipe.getCoinStacks().get(i));
            }
        }

        // 发布槽位设置事件
        PlatformBridge.get().postRecipeSetSlot(builder, recipe, focuses);
    }

    @Override
    public void draw(MillTradeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // 获取对应的建筑 PlanSet 列表（走标准模型以支持准确本地化）
        List<BuildingPlanSet> planSets = MillenaireAPIHelper.getBuildingPlanSetsByShopId(recipe.getCultureId(), recipe.getShopId());

        // 顶行左侧：文化名称
        MutableComponent cultureName = MillenaireLocalizeHelper.getCultureName(recipe.getCultureId());
        guiGraphics.drawString(font, cultureName, 5, 4, 0x404040, false);

        // 顶行右侧：次要交易标签
        if (recipe.getTradeType() == MillTradeRecipe.TradeType.VILLAGE_BUYS_OPTIONAL) {
            Component optionalBuyComp = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TRADE_OPTIONAL_BUY,
                    MillenaireJeimKeys.FALLBACK_TRADE_OPTIONAL_BUY
            );
            int tagWidth = font.width(optionalBuyComp);
            guiGraphics.drawString(font, optionalBuyComp, 155 - tagWidth, 4, 0x888888, false);
        }

        // 第二行：建筑名称与地点计数摘要（正向走建筑本地化名，例如 军匠铺/军械库 而非原始 nativeName）
        Component buildingInfo = buildShopSummaryComponent(recipe.getCultureId(), recipe.getShopId(), planSets);
        guiGraphics.drawString(font, buildingInfo, 5, 15, 0x555555, false);

        // 底行：最低声望要求
        if (recipe.getTradeGood().minReputation() > 0) {
            Component repInfo = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TRADE_MIN_REPUTATION,
                    MillenaireJeimKeys.FALLBACK_TRADE_MIN_REPUTATION,
                    recipe.getTradeGood().minReputation()
            );
            guiGraphics.drawString(font, repInfo, 5, 48, 0xAA0000, false);
        }

        // 建筑详情按钮触发绘制
        boolean defaultBuildingRender = planSets != null && !planSets.isEmpty();
        boolean shouldRender = PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_BUILDING_DETAIL, defaultBuildingRender);
        if (shouldRender) {
            buildingPlusButton.render(guiGraphics, font, BUILDING_BUTTON_X, BUILDING_BUTTON_Y, mouseX, mouseY);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, MillTradeRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 5 && mouseX <= 155 && mouseY >= 2 && mouseY <= 25) {
            List<BuildingPlanSet> planSets = MillenaireAPIHelper.getBuildingPlanSetsByShopId(recipe.getCultureId(), recipe.getShopId());
            if (planSets.size() <= 1) {
                return;
            }

            tooltip.add(Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TOOLTIP_BUILDINGS_HEADER,
                    MillenaireJeimKeys.FALLBACK_TOOLTIP_BUILDINGS_HEADER
            ));

            for (BuildingPlanSet planSet : planSets) {
                Component bName = MillenaireJeimLocalizeHelper.getCraftingBuildingText(planSet);
                MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, bName);
            }
        }
    }

    private Component buildShopSummaryComponent(ResourceLocation cultureId, String shopId, List<BuildingPlanSet> planSets) {
        if (cultureId == null || shopId == null) {
            return Component.translatableWithFallback(MillenaireJeimKeys.KEY_MISSING_SHOP, MillenaireJeimKeys.FALLBACK_MISSING_SHOP);
        }

        if (planSets == null || planSets.isEmpty()) {
            return MillenaireLocalizeHelper.getBuildingName(cultureId, shopId);
        }

        BuildingPlanSet firstPlan = planSets.get(0);
        Component firstBuildingName = MillenaireLocalizeHelper.getBuildingName(firstPlan);

        MutableComponent summary = firstBuildingName.copy();

        int extraPlaces = planSets.size() - 1;
        if (extraPlaces > 0) {
            String countSuffix = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_COUNT_PLACES,
                    MillenaireJeimKeys.FALLBACK_COUNT_PLACES,
                    extraPlaces
            ).getString();
            summary.append(" ").append(countSuffix);
        }

        return summary;
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MillTradeRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(68, 26);

        List<BuildingPlanSet> planSets = MillenaireAPIHelper.getBuildingPlanSetsByShopId(recipe.getCultureId(), recipe.getShopId());
        boolean defaultBuildingRender = planSets != null && !planSets.isEmpty();
        boolean shouldRender = PlatformBridge.get().postRecipeButtonRender(recipe, BUTTON_ID_BUILDING_DETAIL, defaultBuildingRender);

        if (shouldRender) {
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
}
