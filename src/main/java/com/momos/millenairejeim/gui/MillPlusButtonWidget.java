package com.momos.millenairejeim.gui;

import com.momos.millenairejeim.api.PlatformBridge;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 千年工艺通用 GUI 加号按钮控件。
 * 封装原生 Minecraft 风格小按钮的绘制与鼠标交互检测。
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
     * 检查鼠标是否悬停在按钮碰撞箱内。
     */
    public boolean isHovered(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * 响应按钮点击并播放原版 UI 点击音效。
     */
    public void onClick() {
        PlatformBridge.get().playButtonClickSound();
    }

    /**
     * 绘制原生风格按钮及其文本内容。
     */
    public void render(GuiGraphics guiGraphics, Font font, int x, int y, double mouseX, double mouseY) {
        boolean hovered = isHovered(x, y, mouseX, mouseY);

        // 1. 原生微凸起 3D 边框色彩
        int outerTopLeft = hovered ? 0xFFFFFFFF : 0xFF8B8B8B;
        int outerBottomRight = hovered ? 0xFF555555 : 0xFF373737;
        int innerBg = hovered ? 0xFFA0A0A0 : 0xFF6B6B6B;

        // 背景主体
        guiGraphics.fill(x, y, x + width, y + height, innerBg);

        // 高光顶线与左线
        guiGraphics.hLine(x, x + width - 1, y, outerTopLeft);
        guiGraphics.vLine(x, y, y + height - 1, outerTopLeft);

        // 阴影底线与右线
        guiGraphics.hLine(x, x + width - 1, y + height - 1, outerBottomRight);
        guiGraphics.vLine(x + width - 1, y, y + height - 1, outerBottomRight);

        // 2. 居中渲染加号文本 "+"
        String text = "+";
        int textWidth = font.width(text);
        int textX = x + (width - textWidth) / 2 + 1;
        int textY = y + (height - 8) / 2;

        int textColor = hovered ? 0xFFFFFF55 : 0xFFE0E0E0; // 悬停淡金色，常态亮灰
        guiGraphics.drawString(font, text, textX, textY, textColor, false);
    }
}
