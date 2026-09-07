package com.momos.millenairejeim.jei.Trade;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * 支持在物品图标上叠加放大版自定义文本角标（如 '+' / '-'）的 JEI 图标渲染器。
 */
public class BadgedIconDrawable implements IDrawable {
    private final IDrawable itemDrawable;
    private final String badgeText;
    private final int badgeColor;
    private final float textScale;
    private final float offsetX;
    private final float offsetY;

    /**
     * 构造带放大角标的图标渲染器。
     *
     * @param guiHelper  JEI GUI 辅助接口 {@link IGuiHelper}
     * @param itemStack  基础物品堆叠 {@link ItemStack}
     * @param badgeText  角标文本（如 "+" 或 "-"）
     * @param badgeColor 角标颜色 (ARGB/RGB 颜色值)
     * @param textScale  角标字体缩放比例（设置为 1.6F 即长宽放大）
     * @param offsetX    角标相对 X 轴相对偏移
     * @param offsetY    角标相对 Y 轴相对偏移
     */
    public BadgedIconDrawable(IGuiHelper guiHelper, ItemStack itemStack, String badgeText, int badgeColor, float textScale, float offsetX, float offsetY) {
        this.itemDrawable = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, itemStack);
        this.badgeText = badgeText;
        this.badgeColor = badgeColor;
        this.textScale = textScale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public int getWidth() {
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
        // 1. 渲染基础物品图标 (16x16)
        itemDrawable.draw(guiGraphics, xOffset, yOffset);

        // 2. 渲染放大一倍的角标文本
        Font font = Minecraft.getInstance().font;
        guiGraphics.pose().pushPose();

        /**
         * 通过 {@link #offsetX} 与 {@link #offsetY} 动态微调角标位置。
         * offsetY 增大（如 4.0F ~ 5.0F）可将角标向下移动。
         */
        guiGraphics.pose().translate(xOffset + offsetX, yOffset + offsetY, 200.0F);
        guiGraphics.pose().scale(textScale, textScale, 1.0F);

        // 绘制带文字阴影的角标
        guiGraphics.drawString(font, badgeText, 0, 0, badgeColor, true);
        guiGraphics.pose().popPose();
    }
}