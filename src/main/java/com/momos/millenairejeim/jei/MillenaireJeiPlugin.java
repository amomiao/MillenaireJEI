package com.momos.millenairejeim.jei;

import com.mojang.logging.LogUtils;
import com.momos.millenairejeim.jei.Trade.MillTradeCategory;
import com.momos.millenairejeim.jei.Trade.MillTradeRecipe;
import com.momos.millenairejeim.jei.Trade.MillTradeRecipeMaker;
import com.momos.millenairejeim.jei.crafting.MillCraftingCategory;
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipe;
// 新增：动态配方管理器导入
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipeManagerPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
// 新增：JEI 高级注册句柄与配方管理器接口导入
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.item.ModItems;
import org.slf4j.Logger;

/**
 * Millenaire JEI 集成插件入口。
 * 联动 {@link ShopProfileLoader} 与 {@link TradeGoodsLoader} 构建全文化交易链，
 * 并支持从 {@link GoalRegistry} 提取村庄加工与合成配方。
 */
@JeiPlugin
public class MillenaireJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath("millenaire", "jei_plugin");
    public static final RecipeType<MillTradeRecipe> TRADE_SELL_TYPE = RecipeType.create("millenaire", "village_sell", MillTradeRecipe.class);
    public static final RecipeType<MillTradeRecipe> TRADE_BUY_TYPE = RecipeType.create("millenaire", "village_buy", MillTradeRecipe.class);
    public static final RecipeType<MillCraftingRecipe> CRAFTING_TYPE = RecipeType.create("millenaire", "crafting", MillCraftingRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    // `Categories`说白了就是`页签`，这里是在`注册页签`，并且内部实现对JEI页签渲染的描述
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        LOGGER.info("[Millenaire-JEI] 开始注册 JEI Categories...");
        // 注册【千年交易】Category
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        // 千年村庄售出
        registration.addRecipeCategories(new MillTradeCategory(
                guiHelper,
                TRADE_SELL_TYPE,
                Component.translatable("jei.millenaire.category.sell"),
                new ItemStack(ModItems.DENIER_OR.get()),
                true
        ));
        // 千年村庄购入
        registration.addRecipeCategories(new MillTradeCategory(
                guiHelper,
                TRADE_BUY_TYPE,
                Component.translatable("jei.millenaire.category.buy"),
                new ItemStack(ModItems.DENIER_ARGENT.get()),
                false
        ));
        // 注册【千年工艺】Category
        registration.addRecipeCategories(new MillCraftingCategory(
                guiHelper,
                CRAFTING_TYPE,
                Component.translatable("jei.millenaire.category.crafting"),
                new ItemStack(ModItems.DENIER_OR.get())
        ));
        LOGGER.info("[Millenaire-JEI] JEI Categories 注册完成。");
    }

    // 应该在此获取JEI的数据源
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        LOGGER.info("[Millenaire-JEI] 开始构建并注册 JEI 交易配方...");
        // 注册-[千年村庄交易]
        MillTradeRecipeMaker.registerTradeRecipes(registration);
        // 注意-[千年工艺配方]已被动态管理器接管，此处无需再调用 MillCraftingRecipeMaker
    }

    /**
     * 注册 JEI 高级扩展插件。
     * <p>在此处向 {@link IAdvancedRegistration} 注册 {@link IRecipeManagerPlugin} 后，
     * JEI 会自动处理千年工艺配方的按需拉取，不再依赖静态 {@link #registerRecipes(IRecipeRegistration)} 的提前注册。</p>
     * @param registration 高级注册器句柄 {@link IAdvancedRegistration}
     */
    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        LOGGER.info("[Millenaire-JEI] 注册千年工艺动态配方管理器 (MillCraftingRecipeManagerPlugin)...");
        // 查看千年工艺页面时才触发事件
        // 向 JEI 高级注册器中添加自定义配方管理器插件，接管动态懒加载逻辑
        registration.addRecipeManagerPlugin(new MillCraftingRecipeManagerPlugin());
    }
}