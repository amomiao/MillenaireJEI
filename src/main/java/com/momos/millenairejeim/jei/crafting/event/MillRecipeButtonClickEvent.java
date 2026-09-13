package com.momos.millenairejeim.jei.crafting.event;

import com.momos.millenairejeim.jei.IMillRecipe;
import net.neoforged.bus.api.Event;

/**
 * [醒目新注释] 事件2：当用户在 Category GUI 上点击按钮时发布的触发事件。
 */
public class MillRecipeButtonClickEvent extends Event {

    private final IMillRecipe recipe;
    private final String buttonId;
    private final double mouseX;
    private final double mouseY;
    private final int mouseButton;

    public MillRecipeButtonClickEvent(IMillRecipe recipe, String buttonId, double mouseX, double mouseY, int mouseButton) {
        this.recipe = recipe;
        this.buttonId = buttonId;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mouseButton = mouseButton;
    }

    public IMillRecipe getRecipe() {
        return recipe;
    }

    public String getButtonId() {
        return buttonId;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public int getMouseButton() {
        return mouseButton;
    }
}