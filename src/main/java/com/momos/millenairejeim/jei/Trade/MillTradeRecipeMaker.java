package com.momos.millenairejeim.jei.Trade;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.jei.MillenaireJeiPlugin;
import com.momos.millenairejeim.util.MMLog;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 交易配方数据构建与注册器。
 * 负责从 {@link ShopProfileLoader} 和 {@link TradeGoodsLoader} 提取数据，
 * 转换为 JEI 的 {@link MillTradeRecipe} 实例并注册到 {@link IRecipeRegistration} 中。
 */
public final class MillTradeRecipeMaker {

    private MillTradeRecipeMaker() {}

    /**
     * 构建全文化交易配方并提交给 JEI 注册。
     *
     * @param registration JEI 配方注册接口 {@link IRecipeRegistration}
     */
    public static void registerTradeRecipes(IRecipeRegistration registration) {
        List<MillTradeRecipe> sellRecipes = new ArrayList<>();
        List<MillTradeRecipe> buyRecipes = new ArrayList<>();

        /** 使用 {@link MillenaireAPIHelper#getAllCultureIds()} 统一获取全部文化 ResourceLocation */
        Set<ResourceLocation> registeredCultures = MillenaireAPIHelper.getAllCultureIds();
        for (ResourceLocation cultureId : registeredCultures) {
            // [重构] 使用 MMLog 输出处理文化调试日志 (中/英/法)
            MMLog.debug(
                    "[Millenaire-JEI] 正在处理文化: {}",
                    "[Millenaire-JEI] Processing culture: {}",
                    "[Millenaire-JEI] Traitement de la culture : {}",
                    cultureId
            );

            /** 使用 {@link MillenaireAPIHelper#getShopProfiles(ResourceLocation)} 统一获取文化下的商店配置*/
            Map<String, ShopProfile> profiles = MillenaireAPIHelper.getShopProfiles(cultureId);
            if (profiles == null || profiles.isEmpty()) {
                // [重构] 未找到商店配置时警告日志 (中/英/法)
                MMLog.warn(
                        "[Millenaire-JEI] 文化 {} 未找到任何 ShopProfile 数据或尚未加载！",
                        "[Millenaire-JEI] Culture {} has no ShopProfile data found or not yet loaded!",
                        "[Millenaire-JEI] Aucune donnée ShopProfile trouvée ou non encore chargée pour la culture {} !",
                        cultureId
                );
                continue;
            }

            // [重构] 加载商店配置数量调试日志 (中/英/法)
            MMLog.debug(
                    "[Millenaire-JEI] 文化 {} 共加载到 {} 个商店配置",
                    "[Millenaire-JEI] Culture {} loaded {} shop profiles in total",
                    "[Millenaire-JEI] Culture {} : {} profils de magasin chargés au total",
                    cultureId, profiles.size()
            );

            for (Map.Entry<String, ShopProfile> entry : profiles.entrySet()) {
                String shopId = entry.getKey();
                ShopProfile profile = entry.getValue();

                // 1. 构建【售出】配方 (Sells)
                for (String goodId : profile.sells()) {
                    /** 使用 {@link MillenaireAPIHelper#getTradeGood(ResourceLocation, String)} 检索交易商品 */
                    TradeGood good = MillenaireAPIHelper.getTradeGood(cultureId, goodId);
                    if (good != null && good.canSell()) {
                        sellRecipes.add(new MillTradeRecipe(cultureId, shopId, good, MillTradeRecipe.TradeType.VILLAGE_SELLS));
                        MMLog.debug(
                                "[Millenaire-JEI] [售出] 文化: {}, 商店: {}, 商品: {} (价格: {})",
                                "[Millenaire-JEI] [Sell] Culture: {}, Shop: {}, Good: {} (Price: {})",
                                "[Millenaire-JEI] [Vente] Culture : {}, Magasin : {}, Produit : {} (Prix : {})",
                                cultureId, shopId, goodId, good.sellingPrice()
                        );
                    } else if (good == null) {
                        MMLog.warn(
                                "[Millenaire-JEI] [售出] 未能在 TradeGoodsLoader 中找到商品 ID: {} (文化: {}, 商店: {})",
                                "[Millenaire-JEI] [Sell] TradeGood ID {} not found in TradeGoodsLoader (Culture: {}, Shop: {})",
                                "[Millenaire-JEI] [Vente] ID de produit {} non trouvé dans TradeGoodsLoader (Culture : {}, Magasin : {})",
                                goodId, cultureId, shopId
                        );
                    }
                }

                // 2. 构建【紧缺购入】配方 (Buys)
                for (String goodId : profile.buys()) {
                    /** 使用 {@link MillenaireAPIHelper#getTradeGood(ResourceLocation, String)} 检索交易商品。*/
                    TradeGood good = MillenaireAPIHelper.getTradeGood(cultureId, goodId);
                    if (good != null && good.canBuy()) {
                        buyRecipes.add(new MillTradeRecipe(cultureId, shopId, good, MillTradeRecipe.TradeType.VILLAGE_BUYS));
                        MMLog.debug(
                                "[Millenaire-JEI] [购入] 文化: {}, 商店: {}, 商品: {} (收购价: {})",
                                "[Millenaire-JEI] [Buy] Culture: {}, Shop: {}, Good: {} (Buying Price: {})",
                                "[Millenaire-JEI] [Achat] Culture : {}, Magasin : {}, Produit : {} (Prix d'achat : {})",
                                cultureId, shopId, goodId, good.buyingPrice()
                        );
                    } else if (good == null) {
                        MMLog.warn(
                                "[Millenaire-JEI] [购入] 未能在 TradeGoodsLoader 中找到商品 ID: {} (文化: {}, 商店: {})",
                                "[Millenaire-JEI] [Buy] TradeGood ID {} not found in TradeGoodsLoader (Culture: {}, Shop: {})",
                                "[Millenaire-JEI] [Achat] ID de produit {} non trouvé dans TradeGoodsLoader (Culture : {}, Magasin : {})",
                                goodId, cultureId, shopId
                        );
                    }
                }

                // 3. 构建【次要购入】配方 (Buys Optional)
                for (String goodId : profile.buysOptional()) {
                    /** 使用 {@link MillenaireAPIHelper#getTradeGood(ResourceLocation, String)} 检索交易商品。*/
                    TradeGood good = MillenaireAPIHelper.getTradeGood(cultureId, goodId);
                    if (good != null && good.canBuy()) {
                        buyRecipes.add(new MillTradeRecipe(cultureId, shopId, good, MillTradeRecipe.TradeType.VILLAGE_BUYS_OPTIONAL));
                        MMLog.debug(
                                "[Millenaire-JEI] [次要购入] 文化: {}, 商店: {}, 商品: {} (收购价: {})",
                                "[Millenaire-JEI] [Optional Buy] Culture: {}, Shop: {}, Good: {} (Buying Price: {})",
                                "[Millenaire-JEI] [Achat secondaire] Culture : {}, Magasin : {}, Produit : {} (Prix d'achat : {})",
                                cultureId, shopId, goodId, good.buyingPrice()
                        );
                    } else if (good == null) {
                        MMLog.warn(
                                "[Millenaire-JEI] [次要购入] 未能在 TradeGoodsLoader 中找到商品 ID: {} (文化: {}, 商店: {})",
                                "[Millenaire-JEI] [Optional Buy] TradeGood ID {} not found in TradeGoodsLoader (Culture: {}, Shop: {})",
                                "[Millenaire-JEI] [Achat secondaire] ID de produit {} non trouvé dans TradeGoodsLoader (Culture : {}, Magasin : {})",
                                goodId, cultureId, shopId
                        );
                    }
                }
            }
        }

        // [重构] 完成数据解析汇总日志 (中/英/法)
        MMLog.info(
                "[Millenaire-JEI] 交易配方解析完毕！成功生成售出配方 {} 条，购入配方 {} 条。",
                "[Millenaire-JEI] Trade recipe parsing complete! Successfully generated {} sell recipe(s) and {} buy recipe(s).",
                "[Millenaire-JEI] Analyse des recettes de commerce terminée ! Génération réussie de {} recette(s) de vente et {} recette(s) d'achat.",
                sellRecipes.size(), buyRecipes.size()
        );

        registration.addRecipes(MillenaireJeiPlugin.TRADE_SELL_TYPE, sellRecipes);
        registration.addRecipes(MillenaireJeiPlugin.TRADE_BUY_TYPE, buyRecipes);
    }
}