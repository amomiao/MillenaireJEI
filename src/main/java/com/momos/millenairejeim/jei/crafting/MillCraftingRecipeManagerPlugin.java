package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.jei.MillenaireJeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.ItemStack;
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
     * -----------------------------------------------------------------------------------
     * 【重要修正说明】：
     * 上方原注释结论有误。通过 {@link IRecipeManagerPlugin} 自定义接管配方检索时，
     * JEI 不会进行二次自动过滤，必须在此方法内根据 {@link IFocus} 手动筛选输入/输出匹配项！
     * -----------------------------------------------------------------------------------
     *
     * @param recipeCategory JEI 配方分类，参见 {@link IRecipeCategory}
     * @param focus          当前 JEI 聚焦的成分（即玩家在 JEI 中按下 R / U 键查询的物品），参见 {@link IFocus}
     * @param <T>            配方对象类型
     * @param <V>            成分类型
     * @return 经过 Focus（输入/输出/用途）精准匹配后的配方列表
     * @see RecipeIngredientRole
     * @see IFocus#getTypedValue()
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        // 1. 获取当前分类下的全量候选配方
        List<T> allRecipes = getRecipes(recipeCategory);
        if (allRecipes.isEmpty() || focus == null) {
            return allRecipes;
        }

        // 2. 校验 Focus 的成分是否为物品堆叠 {@link ItemStack}
        var typedValue = focus.getTypedValue();
        if (!typedValue.getType().equals(VanillaTypes.ITEM_STACK)) {
            return Collections.emptyList();
        }

        // 提取聚焦查询的物品实例
        ItemStack focusStack = (ItemStack) typedValue.getIngredient();
        if (focusStack == null || focusStack.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 获取当前查询的槽位角色（INPUT 表示按 R 查合成，OUTPUT 表示按 U 查用途）
        RecipeIngredientRole role = focus.getRole();

        // 4. 对配方进行精准过滤
        return (List<T>) allRecipes.stream()
                .filter(r -> r instanceof MillCraftingRecipe)
                .map(r -> (MillCraftingRecipe) r)
                .filter(recipe -> matchesFocus(recipe, focusStack, role))
                .toList();
    }

    /**
     * 校验指定千年配方是否满足 JEI 当前 Focus 的物品与角色匹配关系。
     *
     * @param recipe     待校验的千年配方实体 {@link MillCraftingRecipe}
     * @param focusStack 玩家当前在 JEI 中按键聚焦选中的物品 {@link ItemStack}
     * @param role       当前聚焦的角色类型（如 {@link RecipeIngredientRole#INPUT} 或 {@link RecipeIngredientRole#OUTPUT}）
     * @return 若配方包含该 Focus 物品则返回 {@code true}，否则返回 {@code false}
     */
    private boolean matchesFocus(MillCraftingRecipe recipe, ItemStack focusStack, RecipeIngredientRole role) {
        // 场景 A：玩家按 R 查合成（RecipeIngredientRole.INPUT）
        // 检查配方的输入列表，看是否有任意 Ingredient 能匹配当前聚焦物品
        if (role == RecipeIngredientRole.INPUT) {
            return recipe.getInputs().stream()
                    .anyMatch(input -> input.ingredient().test(focusStack));
        }

        // 场景 B：玩家按 U 查用途（RecipeIngredientRole.OUTPUT）
        // 检查配方的输出列表，看是否有产物与当前聚焦物品属于同一种物品
        if (role == RecipeIngredientRole.OUTPUT) {
            return recipe.getOutputs().stream()
                    .anyMatch(output -> ItemStack.isSameItemSameComponents(output, focusStack));
        }

        // 场景 C：催化剂（CATALYST）或通用检索，同时匹配输入与输出
        boolean matchInput = recipe.getInputs().stream()
                .anyMatch(input -> input.ingredient().test(focusStack));
        boolean matchOutput = recipe.getOutputs().stream()
                .anyMatch(output -> ItemStack.isSameItemSameComponents(output, focusStack));

        return matchInput || matchOutput;
    }
}