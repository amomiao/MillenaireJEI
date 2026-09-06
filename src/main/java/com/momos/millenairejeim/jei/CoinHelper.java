package com.momos.millenairejeim.jei;

import net.minecraft.world.item.ItemStack;
import org.millenaire.commerce.TradeGood;
import org.millenaire.item.ModItems;
import java.util.ArrayList;
import java.util.List;

/**
 * 货币换算工具类。
 * 用于将原始铜钱数值（{@link TradeGood#sellingPrice()} / {@link TradeGood#buyingPrice()}）
 * 按照 1金币 = 64银币 = 4096铜币的换算比例分解为对应的 {@link ItemStack} 列表。
 */
public final class CoinHelper {
    public static final int RATIO_SILVER = 64;
    public static final int RATIO_GOLD = 4096;
    private CoinHelper() {}
    /**
     * 将指定铜币总额拆解为金钱、银钱、铜钱组合。
     *
     * @param totalDenier 铜币总额
     * @return 包含对应面额与数量的 {@link ItemStack} 列表
     */
    public static List<ItemStack> getCoinStacks(int totalDenier) {
        List<ItemStack> coins = new ArrayList<>();
        if (totalDenier <= 0) {
            return coins;
        }
        int gold = totalDenier / RATIO_GOLD;
        int remainderAfterGold = totalDenier % RATIO_GOLD;
        int silver = remainderAfterGold / RATIO_SILVER;
        int bronze = remainderAfterGold % RATIO_SILVER;

        if (gold > 0) {
            coins.add(new ItemStack(ModItems.DENIER_OR.get(), gold));
        }
        if (silver > 0) {
            coins.add(new ItemStack(ModItems.DENIER_ARGENT.get(), silver));
        }
        if (bronze > 0) {
            coins.add(new ItemStack(ModItems.DENIER.get(), bronze));
        }
        return coins;
    }
}