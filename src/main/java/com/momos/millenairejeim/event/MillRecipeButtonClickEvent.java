package com.momos.millenairejeim.event;

import net.neoforged.bus.api.Event;

/**
 * [醒目新注释] 事件2：当用户在 Category GUI 上点击按钮时发布的触发事件。
 * 引入泛型 T 以解耦对特定配方契约 (如 {@link com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe}) 的强硬绑定。
 *
 * @param <T> 触发事件的配方实体类型
 */
public class MillRecipeButtonClickEvent<T> extends Event {

    private final T recipe;
    private final String buttonId;
    private final double mouseX;
    private final double mouseY;
    private final int mouseButton;

    /**
     * [醒目新注释] 构造泛型配方按钮点击事件。
     *
     * @param recipe 触发点击事件的配方实体 {@link T}
     * @param buttonId 按钮标识 ID
     * @param mouseX 鼠标点击 X 坐标
     * @param mouseY 鼠标点击 Y 坐标
     * @param mouseButton 触发点击的鼠标按键索引
     */
    public MillRecipeButtonClickEvent(T recipe, String buttonId, double mouseX, double mouseY, int mouseButton) {
        this.recipe = recipe;
        this.buttonId = buttonId;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mouseButton = mouseButton;
    }

    /**
     * [醒目新注释] 获取触发事件的泛型配方对象。
     *
     * @return 配方实体 {@link T}
     */
    public T getRecipe() {
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