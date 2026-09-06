package com.momos.millenairejeim.jei;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.millenaire.commerce.TradeGood;
import java.util.List;

/**
 * JEI 交易配方数据模型。
 * 包装了 {@link TradeGood}，并承载文化 ID、建筑/商店 ID 以及交易类型（出售/收购/可选收购）。
 */
public class TradeRecipe {
    public enum TradeType {
        VILLAGE_SELLS,   // 村庄售出（玩家购买）
        VILLAGE_BUYS,    // 村庄购入（玩家出售）
        VILLAGE_BUYS_OPTIONAL // 村庄次要购入
    }

    private final ResourceLocation cultureId;
    private final String shopId;
    private final TradeGood tradeGood;
    private final TradeType tradeType;
    private final Ingredient itemIngredient;
    private final List<ItemStack> coinStacks;

    public TradeRecipe(ResourceLocation cultureId, String shopId, TradeGood tradeGood, TradeType tradeType) {
        this.cultureId = cultureId;
        this.shopId = shopId;
        this.tradeGood = tradeGood;
        this.tradeType = tradeType;

        // 解析商品 Ingredient（支持物品 Tag 或直接物品引用）
        if (tradeGood.isTag()) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tradeGood.itemLocation());
            this.itemIngredient = Ingredient.of(tagKey);
        } else {
            Item resolved = tradeGood.resolveItem();
            this.itemIngredient = (resolved != null) ? Ingredient.of(resolved) : Ingredient.EMPTY;
        }

        // 解析货币槽位
        int price = (tradeType == TradeType.VILLAGE_SELLS) ? tradeGood.sellingPrice() : tradeGood.buyingPrice();
        this.coinStacks = CoinHelper.getCoinStacks(price);
    }

    public ResourceLocation getCultureId() { return cultureId; }
    public String getShopId() { return shopId; }
    public TradeGood getTradeGood() { return tradeGood; }
    public TradeType getTradeType() { return tradeType; }
    public Ingredient getItemIngredient() { return itemIngredient; }
    public List<ItemStack> getCoinStacks() { return coinStacks; }
}