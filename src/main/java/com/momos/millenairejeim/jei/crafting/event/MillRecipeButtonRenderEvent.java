package com.momos.millenairejeim.jei.crafting.event;

import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
import net.neoforged.bus.api.Event;

/**
 * [醒目新注释] 事件1：询问特定配方按钮是否应当在 Category GUI 界面上绘制。
 */
public class MillRecipeButtonRenderEvent extends Event {

    private final IMillRecipe recipe;
    private final String buttonId;
    private boolean shouldRender;

    public MillRecipeButtonRenderEvent(IMillRecipe recipe, String buttonId, boolean defaultShouldRender) {
        this.recipe = recipe;
        this.buttonId = buttonId;
        this.shouldRender = defaultShouldRender;
    }

    public IMillRecipe getRecipe() {
        return recipe;
    }

    public String getButtonId() {
        return buttonId;
    }

    public boolean isShouldRender() {
        return shouldRender;
    }

    public void setShouldRender(boolean shouldRender) {
        this.shouldRender = shouldRender;
    }
}