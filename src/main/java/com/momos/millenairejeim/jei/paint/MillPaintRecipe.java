package com.momos.millenairejeim.jei.paint;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.millenaire.block.IPaintedBlock;
import org.millenaire.item.PaintBucketItem;

/**
 * JEI 漆刷/油漆桶染色配方数据模型。
 * 描述通过 {@link PaintBucketItem} 对 {@link IPaintedBlock} 进行染色或重染色的配方关系。
 */
public class MillPaintRecipe {
    private final Ingredient inputBlockIngredient;
    private final ItemStack paintBucketStack;
    private final ItemStack resultStack;
    private final DyeColor targetColor;
    private final Component blockTypeName;

    /**
     * 构建油漆染色配方模型。
     *
     * @param inputBlockIngredient 输入方块原料（包含除目标颜色外的所有变体 {@link Ingredient}）
     * @param paintBucketStack     消耗/使用的油漆桶 {@link ItemStack}
     * @param resultStack          染色产出的目标方块 {@link ItemStack}
     * @param targetColor          目标颜色 {@link DyeColor}
     * @param blockTypeName        方块类型显示名称 {@link Component}
     */
    public MillPaintRecipe(Ingredient inputBlockIngredient, ItemStack paintBucketStack, ItemStack resultStack, DyeColor targetColor, Component blockTypeName) {
        this.inputBlockIngredient = inputBlockIngredient;
        this.paintBucketStack = paintBucketStack;
        this.resultStack = resultStack;
        this.targetColor = targetColor;
        this.blockTypeName = blockTypeName;
    }

    public Ingredient getInputBlockIngredient() { return inputBlockIngredient; }
    public ItemStack getPaintBucketStack() { return paintBucketStack; }
    public ItemStack getResultStack() { return resultStack; }
    public DyeColor getTargetColor() { return targetColor; }
    public Component getBlockTypeName() { return blockTypeName; }
}