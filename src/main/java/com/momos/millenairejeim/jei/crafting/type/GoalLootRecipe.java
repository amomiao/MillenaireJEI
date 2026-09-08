package com.momos.millenairejeim.jei.crafting.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipeManagerPlugin;
import com.momos.millenairejeim.jei.crafting.type.base.AbstractMillCraftingType;
import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.MillBaseRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.ItemHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对应 handler: "mining", "fishing", "fishing_inuit" 等战利品/采掘/钓鱼配方实体。
 * 纯数据模型，渲染与 Tooltip 全量交由 {@link com.momos.millenairejeim.jei.crafting.MillCraftingCategory} 负责。
 */
public class GoalLootRecipe extends MillBaseRecipe {

    public GoalLootRecipe(ResourceLocation id, String goalKey, String handlerId,
                          List<IngredientWithCount> inputs, List<ItemStack> outputs,
                          List<ItemStack> catalysts, List<VillagerType> villagerTypes) {
        super(id, goalKey, handlerId, inputs, outputs, catalysts, villagerTypes);
    }

    /**
     * 战利品/采掘/钓鱼配方静态解析器实现。
     */
    public static class Parser extends AbstractMillCraftingType {

        /**
         * 校验传入的 handlerId 是否属于战利品/采掘类 Handler 集合 {@link MillCraftingRecipeManagerPlugin#LootTypeSet}。
         */
        @Override
        public boolean supports(String handlerId, GatheringType type) {
            return handlerId != null && MillCraftingRecipeManagerPlugin.LootTypeSet.contains(handlerId.toLowerCase());
        }

        /**
         * 解析 {@link GatheringType} 中 handlerParams 的 loot 结构并生成 {@link GoalLootRecipe} 实例。
         */
        @Override
        public IMillRecipe parse(GatheringType type, List<VillagerType> villagers) {
            if (type == null) return null;

            JsonObject params = type.handlerParams();
            List<AbstractMillCraftingType.IngredientWithCount> rawInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();

            // 1. 解析基础输入（工具或来源方块，如有）
            parseInputs(params, rawInputs);

            // 2. 核心逻辑：专门解析 handlerParams 中的 "loot" 数组（形如 [{"item": "...", "count": 1}]）
            if (params != null && params.has("loot")) {
                JsonElement lootElem = params.get("loot");
                if (lootElem.isJsonArray()) {
                    JsonArray lootArray = lootElem.getAsJsonArray();
                    for (JsonElement elem : lootArray) {
                        if (elem.isJsonObject()) {
                            JsonObject lootObj = elem.getAsJsonObject();
                            String itemId = GsonHelper.getAsString(lootObj, JSON_KEY_ITEM, "");
                            if (itemId.isEmpty()) {
                                itemId = GsonHelper.getAsString(lootObj, "id", "");
                            }
                            int count = GsonHelper.getAsInt(lootObj, JSON_KEY_COUNT, 1);
                            Item item = ItemHelper.resolve(itemId);
                            if (item != null && item != Items.AIR) {
                                outputs.add(new ItemStack(item, count));
                            }
                        }
                    }
                }
            }

            // 3. 兜底输出解析：若未查找到 loot 节点，调用基类通用输出提取
            if (outputs.isEmpty()) {
                parseOutputs(type, params, outputs);
            }

            // 若无任何产物且无有效输入，判定为不可解析配方
            if (rawInputs.isEmpty() && outputs.isEmpty()) {
                return null;
            }

            List<IMillRecipe.IngredientWithCount> inputs = rawInputs.stream()
                    .map(raw -> new IMillRecipe.IngredientWithCount(raw.ingredient(), raw.count()))
                    .collect(Collectors.toList());

            String goalKey = type.id() != null ? type.id().getPath() : "unknown";
            String handlerId = type.handlerId();

            return new GoalLootRecipe(type.id(), goalKey, handlerId, inputs, outputs, List.of(), villagers);
        }
    }
}