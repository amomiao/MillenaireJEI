package com.momos.millenairejeim.event;

import net.neoforged.bus.api.Event;

/**
 * [醒目新注释] 事件1：询问特定配方按钮是否应当在 Category GUI 界面上绘制。
 * 引入泛型 T 以解耦对特定配方契约 (如 {@link com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe}) 的强硬绑定。
 *
 * @param <T> 待渲染的配方实体类型
 */
public class MillRecipeButtonRenderEvent<T> extends Event {

    private final T recipe;
    private final String buttonId;
    private boolean shouldRender;

    /**
     * [醒目新注释] 构造泛型配方按钮渲染询问事件。
     *
     * @param recipe 待渲染界面对应的配方实体 {@link T}
     * @param buttonId 按钮标识 ID
     * @param defaultShouldRender 默认绘制状态
     */
    public MillRecipeButtonRenderEvent(T recipe, String buttonId, boolean defaultShouldRender) {
        this.recipe = recipe;
        this.buttonId = buttonId;
        this.shouldRender = defaultShouldRender;
    }

    /**
     * [醒目新注释] 获取待渲染界面对应的泛型配方对象。
     *
     * @return 配方实体 {@link T}
     */
    public T getRecipe() {
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