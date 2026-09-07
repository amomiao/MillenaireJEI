package com.momos.millenairejeim.jei;

import com.momos.millenairejeim.jei.Trade.BadgedIconDrawable;
import com.momos.millenairejeim.jei.Trade.MillTradeCategory;
import com.momos.millenairejeim.jei.Trade.MillTradeRecipe;
import com.momos.millenairejeim.jei.Trade.MillTradeRecipeMaker;
import com.momos.millenairejeim.jei.crafting.MillCraftingCategory;
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipeManagerPlugin;
import com.momos.millenairejeim.jei.paint.MillPaintCategory;
import com.momos.millenairejeim.jei.paint.MillPaintRecipe;
import com.momos.millenairejeim.jei.paint.MillPaintRecipeMaker;
import com.momos.millenairejeim.util.MMLog;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.item.ModItems;
import org.millenaire.item.PaintBucketItem;

/**
 * Millenaire JEI 集成插件入口。
 * 联动 {@link ShopProfileLoader} 与 {@link TradeGoodsLoader} 构建全文化交易链，
 * 并支持从 {@link GoalRegistry} 提取村庄加工与合成配方。
 * <p>已接入 {@link MMLog} 统一进行中/英/法三语控制台日志输出。</p>
 */
@JeiPlugin
public class MillenaireJeiPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath("millenaire", "jei_plugin");
    public static final RecipeType<MillTradeRecipe> TRADE_SELL_TYPE = RecipeType.create("millenaire", "village_sell", MillTradeRecipe.class);
    public static final RecipeType<MillTradeRecipe> TRADE_BUY_TYPE = RecipeType.create("millenaire", "village_buy", MillTradeRecipe.class);
    public static final RecipeType<MillCraftingRecipe> CRAFTING_TYPE = RecipeType.create("millenaire", "crafting", MillCraftingRecipe.class);
    public static final RecipeType<MillPaintRecipe> PAINT_TYPE = RecipeType.create("millenaire", "painting", MillPaintRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    // `Categories`说白了就是`页签`，这里是在`注册页签`，并且内部实现对JEI页签渲染的描述
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        // 使用 MMLog 输出分类注册开始三语日志
        MMLog.info(
                "[Millenaire-JEI] 开始注册 JEI Categories...",
                "[Millenaire-JEI] Starting JEI categories registration...",
                "[Millenaire-JEI] Début de l'enregistrement des catégories JEI..."
        );

        // 注册【千年交易】Category
        // 购入页签：银币 + 红色的 "-"
        IDrawable IconPlayerBuy = new BadgedIconDrawable(guiHelper, new ItemStack(ModItems.DENIER_OR.get()), "+", 0x55FF55,1.6f, 7.0F, 4.5F);
        // 售出页签：金币 + 绿色的 "+"
        IDrawable IconPlayerSeller = new BadgedIconDrawable(guiHelper, new ItemStack(ModItems.DENIER_ARGENT.get()), "-", 0xFF5555,1.8f, 7.0F, 4F);

        // [新注释] 统一改用 {@link MillenaireJeiKeys} 管理的常量 Key 与带 fallback 英文回退文本
        // 千年村庄售出
        registration.addRecipeCategories(new MillTradeCategory(
                guiHelper,
                TRADE_SELL_TYPE,
                Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_CATEGORY_SELL,
                        MillenaireJeiKeys.FALLBACK_CATEGORY_SELL
                ),
                IconPlayerSeller,
                true
        ));
        // 千年村庄购入
        registration.addRecipeCategories(new MillTradeCategory(
                guiHelper,
                TRADE_BUY_TYPE,
                Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_CATEGORY_BUY,
                        MillenaireJeiKeys.FALLBACK_CATEGORY_BUY
                ),
                IconPlayerBuy,
                false
        ));
        // 注册【千年工艺】Category
        registration.addRecipeCategories(new MillCraftingCategory(
                guiHelper,
                CRAFTING_TYPE,
                Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_CATEGORY_CRAFTING,
                        MillenaireJeiKeys.FALLBACK_CATEGORY_CRAFTING
                ),
                new ItemStack(ModItems.TIMBER_FRAME_CROSS_ITEM.get())
        ));
        // 注册【油漆染色】
        registration.addRecipeCategories(new MillPaintCategory(
                guiHelper,
                PAINT_TYPE,
                Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_CATEGORY_PAINTING,
                        MillenaireJeiKeys.FALLBACK_CATEGORY_PAINTING
                ),
                new ItemStack(BuiltInRegistries.ITEM.stream()
                        .filter(PaintBucketItem.class::isInstance)
                        .findFirst()
                        .orElse(ModItems.DENIER_OR.get())) // 获取任意油漆桶作为 Icon
        ));

        // 使用 MMLog 输出分类注册完成三语日志
        MMLog.info(
                "[Millenaire-JEI] JEI Categories 注册完成。",
                "[Millenaire-JEI] JEI categories registration completed.",
                "[Millenaire-JEI] Enregistrement des catégories JEI terminé."
        );
    }

    // 应该在此获取JEI的数据源
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 使用 MMLog 输出交易配方构建三语日志
        MMLog.info(
                "[Millenaire-JEI] 开始构建并注册 JEI 交易配方...",
                "[Millenaire-JEI] Starting building and registering JEI trade recipes...",
                "[Millenaire-JEI] Début de la construction et de l'enregistrement des recettes de commerce JEI..."
        );

        // 注册-[千年村庄交易]
        MillTradeRecipeMaker.registerTradeRecipes(registration);
        // 注意-[千年工艺配方]已被动态管理器接管，此处无需再调用 MillCraftingRecipeMaker
        // ...
        // 注册-[油漆染色]
        MillPaintRecipeMaker.registerPaintRecipes(registration);
    }

    /**
     * 注册 JEI 高级扩展插件。
     * <p>在此处向 {@link IAdvancedRegistration} 注册 {@link IRecipeManagerPlugin} 后，
     * JEI 会自动处理千年工艺配方的按需拉取，不再依赖静态 {@link #registerRecipes(IRecipeRegistration)} 的提前注册。</p>
     * @param registration 高级注册器句柄 {@link IAdvancedRegistration}
     */
    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        // 醒目新增：使用 MMLog 输出高级注册三语日志
        MMLog.info(
                "[Millenaire-JEI] 注册千年工艺动态配方管理器 (MillCraftingRecipeManagerPlugin)...",
                "[Millenaire-JEI] Registering dynamic crafting recipe manager (MillCraftingRecipeManagerPlugin)...",
                "[Millenaire-JEI] Enregistrement du gestionnaire dynamique de recettes de fabrication (MillCraftingRecipeManagerPlugin)..."
        );

        // 查看千年工艺页面时才触发事件
        // 向 JEI 高级注册器中添加自定义配方管理器插件，接管动态懒加载逻辑
        registration.addRecipeManagerPlugin(new MillCraftingRecipeManagerPlugin());
    }
}