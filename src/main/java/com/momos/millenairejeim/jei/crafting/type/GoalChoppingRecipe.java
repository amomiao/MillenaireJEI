package com.momos.millenairejeim.jei.crafting.type;

import com.google.gson.JsonObject;
import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipeManagerPlugin;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.MillCraftingBaseRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对应 handler: "chopping"（伐木与采叶）配方实体。
 * 输出原木与树苗在 JEI 视图中分两组轮播展示，同时全部纳入 getOutputs() 以确保按 R 查询任意木头/树苗均可命中。
 */
public class GoalChoppingRecipe extends MillCraftingBaseRecipe {

    private static final List<Item> DEFAULT_LOGS = List.of(
            Items.OAK_LOG,
            Items.SPRUCE_LOG,
            Items.BIRCH_LOG,
            Items.JUNGLE_LOG,
            Items.ACACIA_LOG,
            Items.DARK_OAK_LOG,
            Items.CHERRY_LOG,
            Items.MANGROVE_LOG
    );

    private static final List<Item> DEFAULT_SAPLINGS = List.of(
            Items.OAK_SAPLING,
            Items.SPRUCE_SAPLING,
            Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING,
            Items.ACACIA_SAPLING,
            Items.DARK_OAK_SAPLING,
            Items.CHERRY_SAPLING,
            Items.MANGROVE_PROPAGULE
    );

    private final List<ItemStack> logOutputs;
    private final List<ItemStack> saplingOutputs;

    public GoalChoppingRecipe(
            ResourceLocation id,
            String goalKey,
            String handlerId,
            List<IngredientWithCount> inputs,
            List<ItemStack> allOutputs,
            List<ItemStack> logOutputs,
            List<ItemStack> saplingOutputs,
            List<VillagerType> villagerTypes
    ) {
        super(id, goalKey, handlerId, inputs, allOutputs, List.of(), villagerTypes);
        this.logOutputs = logOutputs != null ? List.copyOf(logOutputs) : List.of();
        this.saplingOutputs = saplingOutputs != null ? List.copyOf(saplingOutputs) : List.of();
    }

    public List<ItemStack> getLogOutputs() {
        return logOutputs;
    }

    public List<ItemStack> getSaplingOutputs() {
        return saplingOutputs;
    }

    /**
     * 伐木类配方静态解析器实现。
     */
    public static class Parser extends AbstractMillCraftingType {

        @Override
        public boolean supports(String handlerId, GatheringType type) {
            return handlerId != null && MillCraftingRecipeManagerPlugin.ChoppingTypeSet.contains(handlerId.toLowerCase());
        }

        @Override
        public IMillCraftingRecipe parse(GatheringType type, List<VillagerType> villagers) {
            if (type == null) {
                return null;
            }

            JsonObject params = type.handlerParams();
            List<AbstractMillCraftingType.IngredientWithCount> rawInputs = new ArrayList<>();
            parseInputs(params, rawInputs);

            if (rawInputs.isEmpty()) {
                addIngredientByString("#minecraft:axes", 1, rawInputs);
            }

            List<ItemStack> logs = DEFAULT_LOGS.stream().map(ItemStack::new).toList();
            List<ItemStack> saplings = DEFAULT_SAPLINGS.stream().map(ItemStack::new).toList();
            List<ItemStack> allOutputs = new ArrayList<>(logs.size() + saplings.size());
            allOutputs.addAll(logs);
            allOutputs.addAll(saplings);

            List<IMillCraftingRecipe.IngredientWithCount> inputs = rawInputs.stream()
                    .map(raw -> new IMillCraftingRecipe.IngredientWithCount(raw.ingredient(), raw.count()))
                    .collect(Collectors.toList());

            String goalKey = type.id() != null ? type.id().getPath() : "chop_trees";
            GoalChoppingRecipe recipe = new GoalChoppingRecipe(
                    type.id(), goalKey, type.handlerId(), inputs, allOutputs, logs, saplings, villagers
            );

            Component saplingTip = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TOOLTIP_SAPLING_CHANCE,
                    MillenaireJeimKeys.FALLBACK_TOOLTIP_SAPLING_CHANCE,
                    25
            );
            for (Item sapling : DEFAULT_SAPLINGS) {
                recipe.addOutputTooltip(sapling, saplingTip);
            }
            return recipe;
        }
    }
}
