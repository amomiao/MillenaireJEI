package com.momos.millenairejeim.jei.Trade;

import com.momos.millenairejeim.jei.MillenaireJeiKeys;
import com.momos.millenairejeim.util.MillenaireJeimLocalizeHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * JEI 交易 Category 渲染器。
 * 负责渲染 {@link MillTradeRecipe} 的输入输出槽位、商店信息以及声望限制提示。
 */
public class MillTradeCategory implements IRecipeCategory<MillTradeRecipe> {
    private final RecipeType<MillTradeRecipe> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final boolean isSellingCategory;

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
            // [新注释] 售出模式：
            // 左侧货币(n)：以左侧区域中心点 (X=34) 动态居中
            int coinStartX = 34 - (coinCount * 9);
            for (int i = 0; i < coinCount; i++) {
                builder.addSlot(RecipeIngredientRole.INPUT, coinStartX + (i * 18), 26)
                        .addItemStack(recipe.getCoinStacks().get(i));
            }
            // 右侧商品(1)：在右侧区域中心点 (X=126) 居中，左上角起点 X=117
            builder.addSlot(RecipeIngredientRole.OUTPUT, 117, 26)
                    .addIngredients(recipe.getItemIngredient());
        } else {
            // [新注释] 收购模式：
            // 左侧商品(1)：在左侧区域中心点 (X=34) 居中，左上角起点 X=25
            builder.addSlot(RecipeIngredientRole.INPUT, 25, 26)
                    .addIngredients(recipe.getItemIngredient());

            // 右侧货币(n)：以右侧区域中心点 (X=126) 动态居中，3种货币时起点 X=99，完美收纳在右侧区域 (92~160)
            int coinStartX = 126 - (coinCount * 9);
            for (int i = 0; i < coinCount; i++) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, coinStartX + (i * 18), 26)
                        .addItemStack(recipe.getCoinStacks().get(i));
            }
        }
    }

    @Override
    public void draw(MillTradeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        // 渲染文化与商店来源
        Component shopInfo = MillenaireJeimLocalizeHelper.getShopText(recipe.getCultureId(), recipe.getShopId());
        guiGraphics.drawString(font, shopInfo, 5, 4, 0x404040, false);

        // 若为可选购入，放置在第二行（X=5, Y=15）
        if (recipe.getTradeType() == MillTradeRecipe.TradeType.VILLAGE_BUYS_OPTIONAL) {
            Component optionalBuyComp = Component.translatableWithFallback(
                    MillenaireJeiKeys.KEY_TRADE_OPTIONAL_BUY,
                    MillenaireJeiKeys.FALLBACK_TRADE_OPTIONAL_BUY
            );
            guiGraphics.drawString(font, optionalBuyComp, 5, 15, 0x888888, false);
        }

        // 若存在最低声望要求，放置在底部（X=5, Y=48）
        if (recipe.getTradeGood().minReputation() > 0) {
            Component repInfo = Component.translatableWithFallback(
                    MillenaireJeiKeys.KEY_TRADE_MIN_REPUTATION,
                    MillenaireJeiKeys.FALLBACK_TRADE_MIN_REPUTATION,
                    recipe.getTradeGood().minReputation()
            );
            guiGraphics.drawString(font, repInfo, 5, 48, 0xAA0000, false);
        }
    }

    /**
     * [新注释] 保持中央箭头绝对居中：160px 画布中心点 X=80 (起点 X=68, Y=26)
     */
    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MillTradeRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(68, 26);
    }
}