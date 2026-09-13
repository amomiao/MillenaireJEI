package com.momos.millenairejeim.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * [醒目新注释] 千年工艺通用 GUI 加号按钮控件。
 * 封装原生 Minecraft 风格小按钮的绘制与鼠标碰撞检测逻辑。
 */
public class MillPlusButtonWidget {

    private final int width;
    private final int height;

    public MillPlusButtonWidget(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public MillPlusButtonWidget() {
        this(12, 12); // 默认 12x12 小尺寸按钮
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * [醒目新注释] 检查鼠标是否悬停在按钮碰撞箱内
     */
    public boolean isHovered(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * [醒目新注释] 绘制原生风格按钮及其文本内容
     */
    public void render(GuiGraphics guiGraphics, Font font, int x, int y, double mouseX, double mouseY) {
        boolean hovered = isHovered(x, y, mouseX, mouseY);

        // 1. 绘制原生 3D 边框与暗色背景
        int outerBorder = hovered ? 0xFFFFFFFF : 0xFF373737;
        int innerBg = hovered ? 0xFF8B8B8B : 0xFF555555;

        guiGraphics.fill(x, y, x + width, y + height, outerBorder);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, innerBg);

        // 2. 居中绘制绿色 '+' 号文本
        String text = "+";
        int textWidth = font.width(text);
        int textX = x + (width - textWidth) / 2 + 1;
        int textY = y + (height - 8) / 2;

        guiGraphics.drawString(font, text, textX, textY, hovered ? 0x55FF55 : 0x00AA00, false);
    }
}