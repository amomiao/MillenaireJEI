package com.momos.millenairejeim.jei.crafting.view;

import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * 千年工艺视图策略注册中心。
 */
public final class CraftingViewRegistry {
    private static final List<ICraftingLayoutView> VIEWS = new ArrayList<>();
    private static final ICraftingLayoutView FALLBACK_VIEW = new DefaultCraftingLayoutView();

    static {
        VIEWS.add(new ConfigGoalLayoutView());
        VIEWS.add(new EntityInteractionLayoutView());
        VIEWS.add(new HarvestLootLayoutView());
        VIEWS.add(new DefaultCraftingLayoutView());
    }

    private CraftingViewRegistry() {}

    /**
     * 根据配方实体匹配最贴合的视图策略。
     */
    public static ICraftingLayoutView getView(IMillCraftingRecipe recipe) {
        if (recipe == null) {
            return FALLBACK_VIEW;
        }
        for (ICraftingLayoutView view : VIEWS) {
            if (view.supports(recipe)) {
                return view;
            }
        }
        return FALLBACK_VIEW;
    }

    /**
     * 允许外部扩展注册自定义视图策略。
     */
    public static void registerView(ICraftingLayoutView view) {
        if (view != null) {
            VIEWS.add(0, view);
        }
    }
}
