package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.jei.MillenaireJeiPlugin;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import org.millenaire.Millenaire;
import org.millenaire.goal.GoalRegistry;

import java.util.Collections;
import java.util.List;

/**
 * 千年工艺配方 JEI 动态管理器插件（适配 JEI 1.21+ API）。
 */
public class MillCraftingRecipeManagerPlugin implements IRecipeManagerPlugin {
    /**
     * 缓存解析后的配方列表，避免每次 JEI 查询时重复构建。
     */
    private List<MillCraftingRecipe> cachedRecipes = null;

    /**
     * 根据当前聚焦（Focus）获取支持的配方类型列表。
     *
     * @param focus 当前 JEI 聚焦的成分，参见 {@link IFocus}
     * @param <V>   成分类型
     * @return 支持的配方类型 {@link RecipeType} 列表
     */
    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        // 仅在千年的 GoalRegistry 就绪后才向 JEI 宣告支持千年工艺分类
        if (Millenaire.getGoalRegistry() != null) {
            return List.of(MillenaireJeiPlugin.CRAFTING_TYPE);
        }
        return Collections.emptyList();
    }

    // 查看千年工艺页面时才触发事件
    /**
     * 获取指定分类下的全量配方。
     *
     * @param recipeCategory JEI 配方分类，参见 {@link IRecipeCategory}
     * @param <T>            配方对象类型
     * @return 匹配分类的配方列表
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
        // 校验当前 Category 的 RecipeType 是否为千年工艺类型
        if (recipeCategory.getRecipeType().equals(MillenaireJeiPlugin.CRAFTING_TYPE)) {
            GoalRegistry goalRegistry = Millenaire.getGoalRegistry();
            if (goalRegistry != null) {
                if (cachedRecipes == null) {
                    cachedRecipes = MillCraftingRecipeMaker.initRecipes(goalRegistry);
                }
                return (List<T>) cachedRecipes;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 根据 Focus 过滤指定分类下的配方。
     * <p>直接复用全量查询结果，由 JEI 内部自动进行输入/输出匹配。</p>

     *
     * @param recipeCategory JEI 配方分类，参见 {@link IRecipeCategory}
     * @param focus          当前 JEI 聚焦的成分，参见 {@link IFocus}
     * @param <T>            配方对象类型
     * @param <V>            成分类型
     * @return 匹配分类与 Focus 的配方列表
     */
    @Override
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        return getRecipes(recipeCategory);
    }
}