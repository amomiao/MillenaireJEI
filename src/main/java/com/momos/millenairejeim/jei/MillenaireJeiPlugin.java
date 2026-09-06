package com.momos.millenairejeim.jei;

import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.item.ModItems;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Millenaire JEI 集成插件入口。
 * 联动 {@link ShopProfileLoader} 与 {@link TradeGoodsLoader} 构建全文化交易链。
 */
@JeiPlugin
public class MillenaireJeiPlugin implements IModPlugin {
    /**
     * 日志记录器，用于在无法使用断点调试时追踪 JEI 插件加载生命周期与配方注册过程。
     */
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath("millenaire", "jei_plugin");

    public static final RecipeType<TradeRecipe> TRADE_SELL_TYPE =
            RecipeType.create("millenaire", "village_sell", TradeRecipe.class);
    public static final RecipeType<TradeRecipe> TRADE_BUY_TYPE =
            RecipeType.create("millenaire", "village_buy", TradeRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("[Millenaire-JEI] 开始注册 JEI 交易 Category...");
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        // 注册【千年售出】与【千年购入】Category
        registration.addRecipeCategories(new TradeCategory(
                guiHelper,
                TRADE_SELL_TYPE,
                Component.translatable("jei.millenaire.category.sell"),
                new ItemStack(ModItems.DENIER_OR.get()),
                true
        ));

        registration.addRecipeCategories(new TradeCategory(
                guiHelper,
                TRADE_BUY_TYPE,
                Component.translatable("jei.millenaire.category.buy"),
                new ItemStack(ModItems.DENIER_ARGENT.get()),
                false
        ));
        LOGGER.info("[Millenaire-JEI] JEI 交易 Category 注册完成。");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        LOGGER.info("[Millenaire-JEI] 开始构建并注册 JEI 交易配方...");
        List<TradeRecipe> sellRecipes = new ArrayList<>();
        List<TradeRecipe> buyRecipes = new ArrayList<>();

        // 注意：此处需要拿到所有已加载的文化 ID 列表进行遍历
        List<ResourceLocation> registeredCultures = List.of(
                ResourceLocation.fromNamespaceAndPath("millenaire", "norman"),
                ResourceLocation.fromNamespaceAndPath("millenaire", "indian")
        );

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
                        sellRecipes.add(new TradeRecipe(cultureId, shopId, good, TradeRecipe.TradeType.VILLAGE_SELLS));
                        LOGGER.debug("[Millenaire-JEI] [售出] 文化: {}, 商店: {}, 商品: {} (价格: {})", cultureId, shopId, goodId, good.sellingPrice());
                    } else if (good == null) {
                        LOGGER.warn("[Millenaire-JEI] [售出] 未能在 TradeGoodsLoader 中找到商品 ID: {} (文化: {}, 商店: {})", goodId, cultureId, shopId);
                    }
                }

                // 2. 构建【紧缺购入】配方 (Buys)
                for (String goodId : profile.buys()) {
                    TradeGood good = TradeGoodsLoader.getGoodById(cultureId, goodId);
                    if (good != null && good.canBuy()) {
                        buyRecipes.add(new TradeRecipe(cultureId, shopId, good, TradeRecipe.TradeType.VILLAGE_BUYS));
                        LOGGER.debug("[Millenaire-JEI] [购入] 文化: {}, 商店: {}, 商品: {} (收购价: {})", cultureId, shopId, goodId, good.buyingPrice());
                    } else if (good == null) {
                        LOGGER.warn("[Millenaire-JEI] [购入] 未能在 TradeGoodsLoader 中找到商品 ID: {} (文化: {}, 商店: {})", goodId, cultureId, shopId);
                    }
                }

                // 3. 构建【次要购入】配方 (Buys Optional)
                for (String goodId : profile.buysOptional()) {
                    TradeGood good = TradeGoodsLoader.getGoodById(cultureId, goodId);
                    if (good != null && good.canBuy()) {
                        buyRecipes.add(new TradeRecipe(cultureId, shopId, good, TradeRecipe.TradeType.VILLAGE_BUYS_OPTIONAL));
                        LOGGER.debug("[Millenaire-JEI] [次要购入] 文化: {}, 商店: {}, 商品: {} (收购价: {})", cultureId, shopId, goodId, good.buyingPrice());
                    } else if (good == null) {
                        LOGGER.warn("[Millenaire-JEI] [次要购入] 未能在 TradeGoodsLoader 中找到商品 ID: {} (文化: {}, 商店: {})", goodId, cultureId, shopId);
                    }
                }
            }
        }

        LOGGER.info("[Millenaire-JEI] 交易配方解析完毕！成功生成售出配方 {} 条，购入配方 {} 条。", sellRecipes.size(), buyRecipes.size());

        registration.addRecipes(TRADE_SELL_TYPE, sellRecipes);
        registration.addRecipes(TRADE_BUY_TYPE, buyRecipes);
    }
}