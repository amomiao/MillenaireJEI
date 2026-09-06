package com.momos.millenairejeim.jei.Trade;

import com.mojang.logging.LogUtils;
import com.momos.millenairejeim.jei.MillenaireJeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.ModCultures;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 交易配方数据构建与注册器。
 * 负责从 {@link ShopProfileLoader} 和 {@link TradeGoodsLoader} 提取数据，
 * 转换为 JEI 的 {@link MillTradeRecipe} 实例并注册到 {@link IRecipeRegistration} 中。
 */
public final class MillTradeRecipeMaker {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MillTradeRecipeMaker() {}

    /**
     * 构建全文化交易配方并提交给 JEI 注册。
     *
     * @param registration JEI 配方注册接口 {@link IRecipeRegistration}
     */
    public static void registerTradeRecipes(IRecipeRegistration registration) {
        List<MillTradeRecipe> sellRecipes = new ArrayList<>();
        List<MillTradeRecipe> buyRecipes = new ArrayList<>();

        java.util.Set<ResourceLocation> registeredCultures = ModCultures.getAllCultures().keySet();
        for (ResourceLocation cultureId : registeredCultures) {
            LOGGER.debug("[Millenaire-JEI] 正在处理文化: {}", cultureId);
            Map<String, ShopProfile> profiles = ShopProfileLoader.getProfiles(cultureId);
            if (profiles == null || profiles.isEmpty()) {
                LOGGER.warn("[Millenaire-JEI] 文化 {} 未找到任何 ShopProfile 数据或尚未加载！", cultureId);
                continue;
            }
            LOGGER.debug("[Millenaire-JEI] 文化 {} 共加载到 {} 个商店配置", cultureId, profiles.size());
            for (Map.Entry<String, ShopProfile> entry : profiles.entrySet()) {
                String shopId = entry.getKey();
                ShopProfile profile = entry.getValue();

                // 1. 构建【售出】配方 (Sells)
                for (String goodId : profile.sells()) {
                    TradeGood good = TradeGoodsLoader.getGoodById(cultureId, goodId);
                    if (good != null && good.canSell()) {
                        sellRecipes.add(new MillTradeRecipe(cultureId, shopId, good, MillTradeRecipe.TradeType.VILLAGE_SELLS));
                        LOGGER.debug("[Millenaire-JEI] [售出] 文化: {}, 商店: {}, 商品: {} (价格: {})", cultureId, shopId, goodId, good.sellingPrice());
                    } else if (good == null) {
                        LOGGER.warn("[Millenaire-JEI] [售出] 未能在 TradeGoodsLoader 中找到商品 ID: {} (文化: {}, 商店: {})", goodId, cultureId, shopId);
                    }
                }

                // 2. 构建【紧缺购入】配方 (Buys)
                for (String goodId : profile.buys()) {
                    TradeGood good = TradeGoodsLoader.getGoodById(cultureId, goodId);
                    if (good != null && good.canBuy()) {
                        buyRecipes.add(new MillTradeRecipe(cultureId, shopId, good, MillTradeRecipe.TradeType.VILLAGE_BUYS));
                        LOGGER.debug("[Millenaire-JEI] [购入] 文化: {}, 商店: {}, 商品: {} (收购价: {})", cultureId, shopId, goodId, good.buyingPrice());
                    } else if (good == null) {
                        LOGGER.warn("[Millenaire-JEI] [购入] 未能在 TradeGoodsLoader 中找到商品 ID: {} (文化: {}, 商店: {})", goodId, cultureId, shopId);
                    }
                }

                // 3. 构建【次要购入】配方 (Buys Optional)
                for (String goodId : profile.buysOptional()) {
                    TradeGood good = TradeGoodsLoader.getGoodById(cultureId, goodId);
                    if (good != null && good.canBuy()) {
                        buyRecipes.add(new MillTradeRecipe(cultureId, shopId, good, MillTradeRecipe.TradeType.VILLAGE_BUYS_OPTIONAL));
                        LOGGER.debug("[Millenaire-JEI] [次要购入] 文化: {}, 商店: {}, 商品: {} (收购价: {})", cultureId, shopId, goodId, good.buyingPrice());
                    } else if (good == null) {
                        LOGGER.warn("[Millenaire-JEI] [次要购入] 未能在 TradeGoodsLoader 中找到商品 ID: {} (文化: {}, 商店: {})", goodId, cultureId, shopId);
                    }
                }
            }
        }

        LOGGER.info("[Millenaire-JEI] 交易配方解析完毕！成功生成售出配方 {} 条，购入配方 {} 条。", sellRecipes.size(), buyRecipes.size());
        registration.addRecipes(MillenaireJeiPlugin.TRADE_SELL_TYPE, sellRecipes);
        registration.addRecipes(MillenaireJeiPlugin.TRADE_BUY_TYPE, buyRecipes);
    }
}