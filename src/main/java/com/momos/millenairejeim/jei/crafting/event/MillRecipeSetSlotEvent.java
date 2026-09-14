package com.momos.millenairejeim.jei.crafting.event;

import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.neoforged.bus.api.Event;

/**
 * [醒目新注释] 当父模组 {@link com.momos.millenairejeim.jei.crafting.MillCraftingCategory} 绑定配方槽位时发布的事件。
 * 允许拓展子模组 (如 millenairejeimm) 在无需修改父模组源码的情况下，挂载自定义 Ingredient。
 */
public class MillRecipeSetSlotEvent extends Event {

    private final IRecipeLayoutBuilder builder;
    private final IMillCraftingRecipe recipe;
    private final IFocusGroup focuses;

    public MillRecipeSetSlotEvent(IRecipeLayoutBuilder builder, IMillCraftingRecipe recipe, IFocusGroup focuses) {
        this.builder = builder;
        this.recipe = recipe;
        this.focuses = focuses;
    }

    public IRecipeLayoutBuilder getBuilder() {
        return builder;
    }

    public IMillCraftingRecipe getRecipe() {
        return recipe;
    }

    public IFocusGroup getFocuses() {
        return focuses;
    }
}