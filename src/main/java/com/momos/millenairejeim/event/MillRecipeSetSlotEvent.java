package com.momos.millenairejeim.event;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.neoforged.bus.api.Event;

/**
 * [醒目新注释] 当父模组 {@link com.momos.millenairejeim.jei.crafting.MillCraftingCategory} 绑定配方槽位时发布的事件。
 * 允许拓展子模组 (如 millenairejeimm) 在无需修改父模组源码的情况下，挂载自定义 Ingredient。
 * 引入泛型 T 以解耦对特定配方契约 (如 {@link com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe}) 的强硬绑定。
 *
 * @param <T> 槽位绑定的配方实体类型
 */
public class MillRecipeSetSlotEvent<T> extends Event {

    private final IRecipeLayoutBuilder builder;
    private final T recipe;
    private final IFocusGroup focuses;

    /**
     * [醒目新注释] 构造泛型配方槽位绑定事件。
     *
     * @param builder JEI 布局构建器 {@link IRecipeLayoutBuilder}
     * @param recipe 关联的配方实体 {@link T}
     * @param focuses 当前焦点组 {@link IFocusGroup}
     */
    public MillRecipeSetSlotEvent(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        this.builder = builder;
        this.recipe = recipe;
        this.focuses = focuses;
    }

    public IRecipeLayoutBuilder getBuilder() {
        return builder;
    }

    /**
     * [醒目新注释] 获取绑定槽位的泛型配方对象。
     *
     * @return 配方实体 {@link T}
     */
    public T getRecipe() {
        return recipe;
    }

    public IFocusGroup getFocuses() {
        return focuses;
    }
}