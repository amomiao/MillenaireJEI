// region MillTradeCategory 全量代码实现
package com.momos.millenairejeim.jei.Trade;

import com.mojang.blaze3d.platform.InputConstants;
import com.momos.millenairejeim.event.MillRecipeButtonClickEvent;
import com.momos.millenairejeim.event.MillRecipeButtonRenderEvent;
import com.momos.millenairejeim.event.MillRecipeSetSlotEvent;
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
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MillTradeCategory implements IRecipeCategory<MillTradeRecipe> {
    /* =========================================================================================
     * [醒目新注释] 交易加号按钮 ID 标识与绘制位置定义
     * 对应水平位置在右侧 (X=142)，垂直位置 (Y=13) 与第二行建筑文本 (Y=15) 同高对齐。
     * ========================================================================================= */
    public static final String BUTTON_ID_BUILDING_DETAIL = "trade_building_detail_plus";
    private static final int BUILDING_BUTTON_X = 142;
    private static final int BUILDING_BUTTON_Y = 5;

    private final RecipeType<MillTradeRecipe> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final boolean isSellingCategory;

    /* =========================================================================================
     * [醒目新注释] 实例化控件：交易关联建筑右侧的加号按钮实例
     * ========================================================================================= */
    private final MillPlusButtonWidget buildingPlusButton = new MillPlusButtonWidget(12, 12);

    public MillTradeCategory(IGuiHelper guiHelper, RecipeType<MillTradeRecipe> recipeType, Component title, IDrawable icon, boolean isSellingCategory) {
        this.recipeType = recipeType;
        this.title = title;
        this.isSellingCategory = isSellingCategory;
        this.background = guiHelper.createBlankDrawable(160, 60);
        this.icon = icon;
    }

    @Override
    public RecipeType<MillTradeRecipe> getRecipeType() {
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
        return 60;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MillTradeRecipe recipe, IFocusGroup focuses) {
        int coinCount = recipe.getCoinStacks().size();

        if (isSellingCategory) {
            int coinStartX = 34 - (coinCount * 9);
            for (int i = 0; i < coinCount; i++) {
                builder.addSlot(RecipeIngredientRole.INPUT, coinStartX + (i * 18), 26)
                        .addItemStack(recipe.getCoinStacks().get(i));
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 117, 26)
                    .addIngredients(recipe.getItemIngredient());
        } else {
            builder.addSlot(RecipeIngredientRole.INPUT, 25, 26)
                    .addIngredients(recipe.getItemIngredient());

            int coinStartX = 126 - (coinCount * 9);
            for (int i = 0; i < coinCount; i++) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, coinStartX + (i * 18), 26)
                        .addItemStack(recipe.getCoinStacks().get(i));
            }
        }

        /* =========================================================================================
         * [醒目新注释] 事件发布：发布交易配方槽位绑定事件，供后置子模组插入建筑要素轮播槽位
         * ========================================================================================= */
        NeoForge.EVENT_BUS.post(new MillRecipeSetSlotEvent<>(builder, recipe, focuses));
    }

    @Override
    public void draw(MillTradeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        Set<String> buildingKeys = MillenaireAPIHelper.getBuildingKeysByShopId(recipe.getCultureId(), recipe.getShopId());

        /**
         * [新注释] 顶行左侧：使用 {@link MillenaireLocalizeHelper#getCultureName(ResourceLocation)} 绘制文化名称。
         */
        MutableComponent cultureName = MillenaireLocalizeHelper.getCultureName(recipe.getCultureId());
        guiGraphics.drawString(font, cultureName, 5, 4, 0x404040, false);

        /**
         * [新注释] 顶行右侧：若为次要交易，计算组件宽度后在右侧（X=155）右对齐绘制 [Secondary]/[次要] 标识。
         */
        if (recipe.getTradeType() == MillTradeRecipe.TradeType.VILLAGE_BUYS_OPTIONAL) {
            Component optionalBuyComp = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TRADE_OPTIONAL_BUY,
                    MillenaireJeimKeys.FALLBACK_TRADE_OPTIONAL_BUY
            );
            int tagWidth = font.width(optionalBuyComp);
            guiGraphics.drawString(font, optionalBuyComp, 155 - tagWidth, 4, 0x888888, false);
        }

        /**
         * [新注释] 第二行：调用重构后的 {@link #buildShopSummaryComponent(ResourceLocation, String, Set)} 绘制建筑名称及多地点摘要。
         */
        Component buildingInfo = buildShopSummaryComponent(recipe.getCultureId(), recipe.getShopId(), buildingKeys);
        guiGraphics.drawString(font, buildingInfo, 5, 15, 0x555555, false);

        /**
         * [新注释] 底行：独占位置绘制红字声望要求。
         */
        if (recipe.getTradeGood().minReputation() > 0) {
            Component repInfo = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TRADE_MIN_REPUTATION,
                    MillenaireJeimKeys.FALLBACK_TRADE_MIN_REPUTATION,
                    recipe.getTradeGood().minReputation()
            );
            guiGraphics.drawString(font, repInfo, 5, 48, 0xAA0000, false);
        }

        /* =========================================================================================
         * [醒目新注释] 事件1应用：询问是否绘制【交易关联建筑详情查看 (+) 按钮】
         * ========================================================================================= */
        boolean defaultBuildingRender = buildingKeys != null && !buildingKeys.isEmpty();
        MillRecipeButtonRenderEvent<MillTradeRecipe> buildingRenderEvent = new MillRecipeButtonRenderEvent<>(recipe, BUTTON_ID_BUILDING_DETAIL, defaultBuildingRender);
        buildingRenderEvent.setShouldRender(false);
        NeoForge.EVENT_BUS.post(buildingRenderEvent);
        if (buildingRenderEvent.isShouldRender()) {
            buildingPlusButton.render(guiGraphics, font, BUILDING_BUTTON_X, BUILDING_BUTTON_Y, mouseX, mouseY);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, MillTradeRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 5 && mouseX <= 155 && mouseY >= 2 && mouseY <= 25) {
            Set<String> buildingKeys = MillenaireAPIHelper.getBuildingKeysByShopId(recipe.getCultureId(), recipe.getShopId());
            if (buildingKeys.size() <= 1) {
                return;
            }

            tooltip.add(Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TOOLTIP_BUILDINGS_HEADER,
                    MillenaireJeimKeys.FALLBACK_TOOLTIP_BUILDINGS_HEADER
            ));

            for (String bKey : buildingKeys) {
                // [新注释] 传入 buildingKeys 集合自动判断，仅在同组内存在重复译名时才追加 (bKey) 后缀
                Component bName = MillenaireJeimLocalizeHelper.getBuildingText(recipe.getCultureId(), bKey, buildingKeys);
                MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, bName);
            }
        }
    }

    /**
     * [新注释] 重构：取消文化名称前缀拼装，仅构建第二行（建筑名称 + 消歧义ID + 额外地点计数）。
     */
    private Component buildShopSummaryComponent(ResourceLocation cultureId, String shopId, Set<String> buildingKeys) {
        if (cultureId == null || shopId == null) {
            return Component.translatableWithFallback(MillenaireJeimKeys.KEY_MISSING_SHOP, MillenaireJeimKeys.FALLBACK_MISSING_SHOP);
        }

        if (buildingKeys == null || buildingKeys.isEmpty()) {
            return MillenaireLocalizeHelper.getBuildingName(cultureId, shopId);
        }

        List<String> keyList = new ArrayList<>(buildingKeys);
        String firstKey = keyList.get(0);
        Component firstBuildingName = MillenaireLocalizeHelper.getBuildingName(cultureId, firstKey);

        // [新注释] 检测同组建筑中是否有翻译重复的情况
        boolean isDuplicate = MillenaireJeimLocalizeHelper.isNameDuplicate(cultureId, firstKey, buildingKeys);

        MutableComponent summary = firstBuildingName.copy();
        if (isDuplicate) {
            summary.append(" (").append(firstKey).append(")");
        }

        /**
         * [新注释] 若除了当前展示建筑外还有其他地点，计算差值额外地点数，并拼接简短计数格式。
         */
        int extraPlaces = keyList.size() - 1;
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

    /**
     * [醒目新注释] JEI 19.x+ 声明式输入处理器挂载方法。
     * 为交易建筑详情加号按钮挂载 {@link IJeiInputHandler}。
     */
    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MillTradeRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(68, 26);

        // [醒目新注释] 交易关联建筑加号按钮交互响应挂载
        Set<String> buildingKeys = MillenaireAPIHelper.getBuildingKeysByShopId(recipe.getCultureId(), recipe.getShopId());
        boolean defaultBuildingRender = buildingKeys != null && !buildingKeys.isEmpty();
        MillRecipeButtonRenderEvent<MillTradeRecipe> buildingRenderEvent = new MillRecipeButtonRenderEvent<>(recipe, BUTTON_ID_BUILDING_DETAIL, defaultBuildingRender);
        NeoForge.EVENT_BUS.post(buildingRenderEvent);

        if (buildingRenderEvent.isShouldRender()) {
            builder.addInputHandler(new IJeiInputHandler() {
                @Override
                public ScreenRectangle getArea() {
                    return new ScreenRectangle(BUILDING_BUTTON_X, BUILDING_BUTTON_Y, buildingPlusButton.getWidth(), buildingPlusButton.getHeight());
                }

                @Override
                public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    InputConstants.Key key = input.getKey();
                    if (key.getType() == InputConstants.Type.MOUSE && key.getValue() == 0) { // 0 代表鼠标左键
                        NeoForge.EVENT_BUS.post(new MillRecipeButtonClickEvent<>(recipe, BUTTON_ID_BUILDING_DETAIL, mouseX, mouseY, 0));
                        return true;
                    }
                    return false;
                }
            });
        }
    }
}
// endregion MillTradeCategory 全量代码实现