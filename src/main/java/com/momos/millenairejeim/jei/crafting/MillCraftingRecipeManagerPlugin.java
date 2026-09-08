package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.jei.MillenaireJeiPlugin;
import com.momos.millenairejeim.jei.crafting.type.GoalCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.AbstractMillCraftingType;
import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.MillBaseRecipe;
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
import java.util.Set;

/**
 * 千年工艺配方 JEI 动态管理器插件（适配 JEI 1.21+ API，单页签全多态支持）。
 */
public class MillCraftingRecipeManagerPlugin implements IRecipeManagerPlugin {
    /** 1. 合成和熔炉都是需要input/output的对象直接放在一起渲染 */
    public static final Set<String> CraftTypeSet = Set.of(
            "crafting",
            "smelting"
    );
    /** 2.战利品表:依赖item和count字段及其战利品表 */
    public static final Set<String> LootTypeSet = Set.of(
            "mining",
            "fishing",
            "fishing_inuit"
    );
    /** 3.各种收割行为 */
    public static final Set<String> HarvestTypeSet = Set.of(
            "harvesting",
            "fruit_harvesting",
            "cocoa_harvesting"
    );
    /** 4.与实体的各种行为 */
    public static final Set<String> OfEntityTypeSet =Set.of(
            "slaughter",
            "breeding",
            "shearing"
    );

    private List<IMillRecipe> cachedRecipes = null;

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        if (Millenaire.getGoalRegistry() != null) {
            // 仅暴露唯一的工艺页签
            return List.of(MillenaireJeiPlugin.CRAFTING_TYPE);
        }
        return Collections.emptyList();
    }

    /// 0.0-JEI显示的总入口 [#getRecipes(IRecipeCategory<T>,IFocus<V>)]
    /// 0.1-本方法处理获取页签下的所有对象 [#getRecipes(IRecipeCategory<T>)]
    /// 0.2-入口会进行过滤找到查询对象 [#getRecipes(IRecipeCategory<T>,IFocus<V>)]
    /// 首次查询
    /// 1.1-初始化入口 [MillCraftingRecipeMaker#initRecipes]
    /// 1.2-对Goal进行解析 [MillCraftingRecipeMaker#getRecipesByHandlers],`Goal`类型与对应类型匹配如[#CraftTypeSet]的对象会被解析
    /// 1.3-得到被激活的有效`Goal`[GoalRegistry#getGatheringGoals]
    /// 1.4-遍历对每个`Goal`解析,但此方法只是一个分发[MillCraftingRecipe#parse]
    /// 1.4.1-如[GoalCraftingRecipe],他的父类为[MillBaseRecipe]祖类为[IMillRecipe],他有内部类[GoalCraftingRecipe.Parser]此内部类继承[AbstractMillCraftingType]
    /// 1.4.2-[MillCraftingRecipe#parse]分发时会调用[GoalCraftingRecipe.Parser#supports]检查解析是否由[GoalCraftingRecipe]进行
    /// 1.4.3-通过[GoalCraftingRecipe.Parser#parse]完成一条内容的解析，方法是`非空`并且匹配[#CraftTypeSet]
    /// 1.5-遍历完成获得解析结果 [#cachedRecipes]
    /// 2.1-告知渲染对象 [#getRecipes(IRecipeCategory<T>,IFocus<V>)]
    /// 2.2-通过预设,渲染一些内容 [MillCraftingCategory#setRecipe]
    /// 2.3-通过自定义,渲染一些内容 [MillCraftingCategory#draw]
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
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

    @Override
    @SuppressWarnings("unchecked")
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        List<T> allRecipes = getRecipes(recipeCategory);
        if (allRecipes.isEmpty() || focus == null) {
            return allRecipes;
        }
        var typedValue = focus.getTypedValue();
        if (!typedValue.getType().equals(VanillaTypes.ITEM_STACK)) {
            return Collections.emptyList();
        }
        ItemStack focusStack = (ItemStack) typedValue.getIngredient();
        if (focusStack == null || focusStack.isEmpty()) {
            return Collections.emptyList();
        }
        RecipeIngredientRole role = focus.getRole();
        return (List<T>) allRecipes.stream()
                .filter(r -> r instanceof IMillRecipe)
                .map(r -> (IMillRecipe) r)
                .filter(recipe -> matchesFocus(recipe, focusStack, role))
                .toList();
    }

    /** [新注释] 修正形参类型为 {@link IMillRecipe} 接口，兼容单一页签下的所有多态工艺类。*/
    private boolean matchesFocus(IMillRecipe recipe, ItemStack focusStack, RecipeIngredientRole role) {
        if (role == RecipeIngredientRole.INPUT) {
            return recipe.getInputs().stream()
                    .anyMatch(input -> input.ingredient().test(focusStack));
        }
        if (role == RecipeIngredientRole.OUTPUT) {
            return recipe.getOutputs().stream()
                    .anyMatch(output -> ItemStack.isSameItemSameComponents(output, focusStack));
        }
        boolean matchInput = recipe.getInputs().stream()
                .anyMatch(input -> input.ingredient().test(focusStack));
        boolean matchOutput = recipe.getOutputs().stream()
                .anyMatch(output -> ItemStack.isSameItemSameComponents(output, focusStack));
        return matchInput || matchOutput;
    }
}