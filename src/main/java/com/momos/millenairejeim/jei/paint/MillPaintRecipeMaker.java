package com.momos.millenairejeim.jei.paint;

import com.momos.millenairejeim.jei.MillenaireJeiKeys;
import com.momos.millenairejeim.jei.MillenaireJeiPlugin;
import com.momos.millenairejeim.util.MMLog;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.millenaire.block.ModBlocks;
import org.millenaire.item.PaintBucketItem;

import java.util.*;

/**
 * 油漆染色配方构建与注册工具类。
 * 遍历 {@link ModBlocks} 注册表提取可刷漆方块，匹配 {@link PaintBucketItem} 生成 JEI 配方。
 */
public final class MillPaintRecipeMaker {

    private MillPaintRecipeMaker() {}

    /**
     * 自动提取所有油漆桶与染色方块变体，构建配方并注册至 JEI。
     *
     * @param registration JEI 配方注册句柄 {@link IRecipeRegistration}
     */
    public static void registerPaintRecipes(IRecipeRegistration registration) {
        List<MillPaintRecipe> recipes = new ArrayList<>();

        // 1. 获取注册表中所有 DyeColor 对应的 PaintBucketItem
        Map<DyeColor, ItemStack> paintBuckets = getPaintBucketMap();

        // 2. [新注释] 映射 ModBlocks 中支持 DyeColor 的 5 种方块族，改用 {@link MillenaireJeiKeys} 统一管理的 Key 与带 fallback 回退英文
        List<BlockFamily> blockFamilies = List.of(
                new BlockFamily(Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_PAINT_PAINTED_BRICKS,
                        MillenaireJeiKeys.FALLBACK_PAINT_PAINTED_BRICKS
                ), ModBlocks.PAINTED_BRICKS),

                new BlockFamily(Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_PAINT_DECORATED_BRICKS,
                        MillenaireJeiKeys.FALLBACK_PAINT_DECORATED_BRICKS
                ), ModBlocks.DECORATED_BRICKS),

                new BlockFamily(Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_PAINT_PAINTED_BRICK_STAIRS,
                        MillenaireJeiKeys.FALLBACK_PAINT_PAINTED_BRICK_STAIRS
                ), ModBlocks.PAINTED_BRICK_STAIRS),

                new BlockFamily(Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_PAINT_PAINTED_BRICK_SLABS,
                        MillenaireJeiKeys.FALLBACK_PAINT_PAINTED_BRICK_SLABS
                ), ModBlocks.PAINTED_BRICK_SLABS),

                new BlockFamily(Component.translatableWithFallback(
                        MillenaireJeiKeys.KEY_PAINT_PAINTED_BRICK_WALLS,
                        MillenaireJeiKeys.FALLBACK_PAINT_PAINTED_BRICK_WALLS
                ), ModBlocks.PAINTED_BRICK_WALLS)
        );

        // 3. 循环 16 种颜色生成配方
        for (DyeColor targetColor : DyeColor.values()) {
            ItemStack bucketStack = paintBuckets.get(targetColor);
            if (bucketStack == null || bucketStack.isEmpty()) {
                continue;
            }

            for (BlockFamily family : blockFamilies) {
                DeferredBlock<?> targetDeferredBlock = family.map().get(targetColor);
                if (targetDeferredBlock == null) {
                    continue;
                }

                ItemStack resultStack = new ItemStack((Block) targetDeferredBlock.get());

                // 收集除目标颜色外的所有其他颜色方块，组合为可循环展示的 Ingredient
                List<ItemStack> inputBlocks = new ArrayList<>();
                for (DyeColor sourceColor : DyeColor.values()) {
                    if (sourceColor == targetColor) {
                        continue;
                    }
                    DeferredBlock<?> sourceDeferredBlock = family.map().get(sourceColor);
                    if (sourceDeferredBlock != null) {
                        inputBlocks.add(new ItemStack((Block) sourceDeferredBlock.get()));
                    }
                }

                if (!inputBlocks.isEmpty()) {
                    Ingredient inputIngredient = Ingredient.of(inputBlocks.toArray(new ItemStack[0]));
                    recipes.add(new MillPaintRecipe(
                            inputIngredient,
                            bucketStack,
                            resultStack,
                            targetColor,
                            family.displayName()
                    ));
                }
            }
        }

        MMLog.info(
                "[Millenaire-JEI] 成功生成油漆染色配方 {} 条。",
                "[Millenaire-JEI] Successfully generated {} paint recipes.",
                "[Millenaire-JEI] {} recettes de peinture générées avec succès.",
                recipes.size()
        );

        registration.addRecipes(MillenaireJeiPlugin.PAINT_TYPE, recipes);
    }

    /**
     * 遍历物品注册表提取各 {@link DyeColor} 对应的 {@link PaintBucketItem}。
     */
    private static Map<DyeColor, ItemStack> getPaintBucketMap() {
        Map<DyeColor, ItemStack> map = new EnumMap<>(DyeColor.class);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof PaintBucketItem bucketItem) {
                map.put(bucketItem.getColor(), new ItemStack(bucketItem));
            }
        }
        return map;
    }

    /**
     * 方块族数据结构记录类。
     */
    private record BlockFamily(Component displayName, Map<DyeColor, ? extends DeferredBlock<?>> map) {}
}