package com.momos.millenairejeim.jei.Trade;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.MillenaireJeiKeys;
import com.momos.millenairejeim.util.MillenaireJeimLocalizeHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    }

    @Override
    public void draw(MillTradeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        Set<String> buildingKeys = MillenaireAPIHelper.getBuildingKeysByShopId(recipe.getCultureId(), recipe.getShopId());
        Component shopInfo = buildShopSummaryComponent(recipe.getCultureId(), recipe.getShopId(), buildingKeys);

        guiGraphics.drawString(font, shopInfo, 5, 4, 0x404040, false);

        if (recipe.getTradeType() == MillTradeRecipe.TradeType.VILLAGE_BUYS_OPTIONAL) {
            Component optionalBuyComp = Component.translatableWithFallback(
                    MillenaireJeiKeys.KEY_TRADE_OPTIONAL_BUY,
                    MillenaireJeiKeys.FALLBACK_TRADE_OPTIONAL_BUY
            );
            guiGraphics.drawString(font, optionalBuyComp, 5, 15, 0x888888, false);
        }

        if (recipe.getTradeGood().minReputation() > 0) {
            Component repInfo = Component.translatableWithFallback(
                    MillenaireJeiKeys.KEY_TRADE_MIN_REPUTATION,
                    MillenaireJeiKeys.FALLBACK_TRADE_MIN_REPUTATION,
                    recipe.getTradeGood().minReputation()
            );
            guiGraphics.drawString(font, repInfo, 5, 48, 0xAA0000, false);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, MillTradeRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 5 && mouseX <= 155 && mouseY >= 2 && mouseY <= 14) {
            Set<String> buildingKeys = MillenaireAPIHelper.getBuildingKeysByShopId(recipe.getCultureId(), recipe.getShopId());
            if (buildingKeys.size() <= 1) {
                return;
            }

            tooltip.add(Component.translatableWithFallback(
                    MillenaireJeiKeys.KEY_TOOLTIP_BUILDINGS_HEADER,
                    MillenaireJeiKeys.FALLBACK_TOOLTIP_BUILDINGS_HEADER
            ));

            for (String bKey : buildingKeys) {
                // [新注释] 传入 buildingKeys 集合自动判断，仅在同组内存在重复译名时才追加 (bKey) 后缀
                Component bName = MillenaireJeimLocalizeHelper.getBuildingText(recipe.getCultureId(), bKey, buildingKeys);
                MillenaireJeimLocalizeHelper.addTooltipEntry(tooltip, bName);
            }
        }
    }

    /**
     * [新注释] 构建交易界面顶部的商店/建筑摘要。
     * 自动检测是否存在译名重名，无重名时直接显示名称，不再拼接括号后缀。
     *
     * @param cultureId    文化 {@link ResourceLocation}
     * @param shopId       商店 ID
     * @param buildingKeys 关联的建筑 Key 集合
     * @return 格式化的摘要组件 {@link Component}
     */
    private Component buildShopSummaryComponent(ResourceLocation cultureId, String shopId, Set<String> buildingKeys) {
        if (cultureId == null || shopId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_SHOP, MillenaireJeiKeys.FALLBACK_MISSING_SHOP);
        }

        MutableComponent cultureName = MillenaireLocalizeHelper.getCultureName(cultureId);

        if (buildingKeys == null || buildingKeys.isEmpty()) {
            return cultureName.append(" - ")
                    .append(MillenaireLocalizeHelper.getBuildingName(cultureId, shopId));
        }

        List<String> keyList = new ArrayList<>(buildingKeys);
        String firstKey = keyList.get(0);
        Component firstBuildingName = MillenaireLocalizeHelper.getBuildingName(cultureId, firstKey);

        // [新注释] 检测同组建筑中是否有翻译重复的情况
        boolean isDuplicate = MillenaireJeimLocalizeHelper.isNameDuplicate(cultureId, firstKey, buildingKeys);

        MutableComponent summary = cultureName.append(" - ").append(firstBuildingName);
        if (isDuplicate) {
            summary.append(" (").append(firstKey).append(")");
        }

        if (keyList.size() > 1) {
            String countSuffix = Component.translatableWithFallback(
                    MillenaireJeiKeys.KEY_COUNT_PLACES,
                    MillenaireJeiKeys.FALLBACK_COUNT_PLACES,
                    keyList.size()
            ).getString();
            summary.append(" ").append(countSuffix);
        }

        return summary;
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MillTradeRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(68, 26);
    }
}