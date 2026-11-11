package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.config.MillJeiConfigManager;
import com.momos.millenairejeim.jei.MillenaireJeimPlugin;
import com.momos.millenairejeim.jei.crafting.type.GoalConfigRecipe;
import com.momos.millenairejeim.jei.crafting.type.GoalHarvestRecipe;
import com.momos.millenairejeim.jei.crafting.type.GoalSpotRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.ItemStack;
import org.millenaire.Millenaire;
import org.millenaire.goal.GoalRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 千年工艺、定点采集及外部配置目标 JEI 动态管理器插件（适配 JEI 1.21+ API）。
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
            "cocoa_harvesting",
            "paddy_harvesting"
    );
    /** 3b.伐木与采叶行为 */
    public static final Set<String> ChoppingTypeSet = Set.of(
            "chopping"
    );
    /** 4.与实体的各种行为 */
    public static final Set<String> OfEntityTypeSet = Set.of(
            "slaughter",
            "breeding",
            "shearing"
    );

    private GoalRegistry lastGoalRegistry = null;
    private long lastConfigGeneration = -1L;
    private List<IMillCraftingRecipe> cachedCraftingRecipes = null;
    private List<GoalSpotRecipe> cachedSpotRecipes = null;
    private List<GoalConfigRecipe> cachedCustomRecipes = null;

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        GoalRegistry goalRegistry = Millenaire.getGoalRegistry();
        if (focus == null) {
            if (goalRegistry == null) {
                return List.of(MillenaireJeimPlugin.CUSTOM_GOALS_TYPE);
            }
            return List.of(
                    MillenaireJeimPlugin.CRAFTING_TYPE,
                    MillenaireJeimPlugin.SPOT_GATHERING_TYPE,
                    MillenaireJeimPlugin.CUSTOM_GOALS_TYPE
            );
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
        List<RecipeType<?>> matchedTypes = new ArrayList<>(3);
        if (goalRegistry != null) {
            if (getOrLoadCraftingRecipes(goalRegistry).stream().anyMatch(recipe -> matchesFocus(recipe, focusStack, role))) {
                matchedTypes.add(MillenaireJeimPlugin.CRAFTING_TYPE);
            }
            if (getOrLoadSpotRecipes(goalRegistry).stream().anyMatch(recipe -> matchesFocus(recipe, focusStack, role))) {
                matchedTypes.add(MillenaireJeimPlugin.SPOT_GATHERING_TYPE);
            }
        }
        if (getOrLoadCustomRecipes(goalRegistry).stream().anyMatch(recipe -> matchesFocus(recipe, focusStack, role))) {
            matchedTypes.add(MillenaireJeimPlugin.CUSTOM_GOALS_TYPE);
        }
        return matchedTypes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
        GoalRegistry goalRegistry = Millenaire.getGoalRegistry();
        RecipeType<T> type = recipeCategory.getRecipeType();
        if (type.equals(MillenaireJeimPlugin.CUSTOM_GOALS_TYPE)) {
            return (List<T>) getOrLoadCustomRecipes(goalRegistry);
        }
        if (goalRegistry == null) {
            return Collections.emptyList();
        }
        if (type.equals(MillenaireJeimPlugin.CRAFTING_TYPE)) {
            return (List<T>) getOrLoadCraftingRecipes(goalRegistry);
        }
        if (type.equals(MillenaireJeimPlugin.SPOT_GATHERING_TYPE)) {
            return (List<T>) getOrLoadSpotRecipes(goalRegistry);
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
                .filter(r -> r instanceof IMillCraftingRecipe)
                .map(r -> (IMillCraftingRecipe) r)
                .filter(recipe -> matchesFocus(recipe, focusStack, role))
                .toList();
    }

    private void ensureRegistryFresh(GoalRegistry goalRegistry) {
        long currentGen = MillJeiConfigManager.getCacheGeneration();
        if (goalRegistry != lastGoalRegistry || currentGen != lastConfigGeneration) {
            cachedCraftingRecipes = null;
            cachedSpotRecipes = null;
            cachedCustomRecipes = null;
            lastGoalRegistry = goalRegistry;
            lastConfigGeneration = currentGen;
        }
    }

    private List<IMillCraftingRecipe> getOrLoadCraftingRecipes(GoalRegistry goalRegistry) {
        ensureRegistryFresh(goalRegistry);
        if (cachedCraftingRecipes == null) {
            cachedCraftingRecipes = MillCraftingRecipeMaker.initStandardCraftingRecipes(goalRegistry);
        }
        return cachedCraftingRecipes;
    }

    private List<GoalSpotRecipe> getOrLoadSpotRecipes(GoalRegistry goalRegistry) {
        ensureRegistryFresh(goalRegistry);
        if (cachedSpotRecipes == null) {
            cachedSpotRecipes = MillCraftingRecipeMaker.getSpotGatheringRecipes(goalRegistry);
        }
        return cachedSpotRecipes;
    }

    private List<GoalConfigRecipe> getOrLoadCustomRecipes(GoalRegistry goalRegistry) {
        ensureRegistryFresh(goalRegistry);
        if (cachedCustomRecipes == null) {
            cachedCustomRecipes = MillCraftingRecipeMaker.getCustomGoalRecipes(goalRegistry);
        }
        return cachedCustomRecipes;
    }

    /** 兼容普通工艺、定点采集与纯配置型多态配方类的焦点检索匹配。*/
    private boolean matchesFocus(IMillCraftingRecipe recipe, ItemStack focusStack, RecipeIngredientRole role) {
        if (role == RecipeIngredientRole.INPUT) {
            return recipe.getInputs().stream()
                    .anyMatch(input -> input.ingredient().test(focusStack));
        }
        if (role == RecipeIngredientRole.CATALYST) {
            return recipe.getCatalysts().stream()
                    .anyMatch(catalyst -> ItemStack.isSameItemSameComponents(catalyst, focusStack));
        }
        if (role == RecipeIngredientRole.OUTPUT) {
            return recipe.getOutputs().stream()
                    .anyMatch(output -> ItemStack.isSameItemSameComponents(output, focusStack))
                    || (recipe instanceof GoalHarvestRecipe harvest && harvest.hasIrrigationBonus()
                    && ItemStack.isSameItemSameComponents(harvest.getIrrigationBonusCrop(), focusStack));
        }
        boolean matchInput = recipe.getInputs().stream()
                .anyMatch(input -> input.ingredient().test(focusStack));
        boolean matchOutput = recipe.getOutputs().stream()
                .anyMatch(output -> ItemStack.isSameItemSameComponents(output, focusStack))
                || (recipe instanceof GoalHarvestRecipe harvest && harvest.hasIrrigationBonus()
                && ItemStack.isSameItemSameComponents(harvest.getIrrigationBonusCrop(), focusStack));
        boolean matchCatalyst = recipe.getCatalysts().stream()
                .anyMatch(catalyst -> ItemStack.isSameItemSameComponents(catalyst, focusStack));
        return matchInput || matchCatalyst || matchOutput;
    }
}
