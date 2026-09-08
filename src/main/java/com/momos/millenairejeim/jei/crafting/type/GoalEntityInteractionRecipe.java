package com.momos.millenairejeim.jei.crafting.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipeManagerPlugin;
import com.momos.millenairejeim.jei.crafting.type.base.AbstractMillCraftingType;
import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.MillBaseRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.ItemHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 对应 handler: "slaughter", "breeding", "shearing" 等与实体互动（屠宰、繁殖、剪毛）的配方实体。
 */
public class GoalEntityInteractionRecipe extends MillBaseRecipe {

    // region 预留扩展字段
    private final String targetEntityType;
    private final ItemStack entitySpawnEgg;
    private final Integer maxEntityCount;
    private final Integer minEntityCount;
    private final String actionType; // "slaughter", "breeding", "shearing"
    // endregion 预留扩展字段

    public GoalEntityInteractionRecipe(ResourceLocation id, String goalKey, String handlerId,
                                       List<IngredientWithCount> inputs, List<ItemStack> outputs,
                                       List<ItemStack> catalysts, List<VillagerType> villagerTypes,
                                       String targetEntityType, ItemStack entitySpawnEgg,
                                       Integer maxEntityCount, Integer minEntityCount, String actionType) {
        super(id, goalKey, handlerId, inputs, outputs, catalysts, villagerTypes);
        this.targetEntityType = targetEntityType;
        this.entitySpawnEgg = entitySpawnEgg != null ? entitySpawnEgg : ItemStack.EMPTY;
        this.maxEntityCount = maxEntityCount;
        this.minEntityCount = minEntityCount;
        this.actionType = actionType;
    }

    public String getTargetEntityType() { return targetEntityType; }
    public ItemStack getEntitySpawnEgg() { return entitySpawnEgg; }
    public Integer getMaxEntityCount() { return maxEntityCount; }
    public Integer getMinEntityCount() { return minEntityCount; }
    public String getActionType() { return actionType; }
    public boolean hasSpawnEgg() { return !entitySpawnEgg.isEmpty(); }

    /**
     * 实体交互类配方静态解析器实现。
     */
    public static class Parser extends AbstractMillCraftingType {

        @Override
        public boolean supports(String handlerId, GatheringType type) {
            return handlerId != null && MillCraftingRecipeManagerPlugin.OfEntityTypeSet.contains(handlerId.toLowerCase());
        }

        @Override
        public IMillRecipe parse(GatheringType type, List<VillagerType> villagers) {
            if (type == null) return null;

            JsonObject params = type.handlerParams();
            List<AbstractMillCraftingType.IngredientWithCount> rawInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            List<ItemStack> catalysts = new ArrayList<>();

            // 1. 基础输入/工具解析（兼容常规 inputs 声明）
            parseInputs(params, rawInputs);

            String targetEntityStr = "";
            ItemStack spawnEggStack = ItemStack.EMPTY;
            Integer maxCount = null;
            Integer minCount = null;
            String handlerId = type.handlerId() != null ? type.handlerId().toLowerCase() : "";

            if (params != null) {
                // 2. 提取目标实体 ID（优先解析 "animalType"，其次兼容 "entity", "entityType", "targetEntity" 以及 "buildingTag" 回退）
                if (params.has("animalType")) {
                    targetEntityStr = GsonHelper.getAsString(params, "animalType", "");
                } else if (params.has("entity")) {
                    targetEntityStr = GsonHelper.getAsString(params, "entity", "");
                } else if (params.has("entityType")) {
                    targetEntityStr = GsonHelper.getAsString(params, "entityType", "");
                } else if (params.has("targetEntity")) {
                    targetEntityStr = GsonHelper.getAsString(params, "targetEntity", "");
                } else if (params.has("buildingTag")) {
                    String tag = GsonHelper.getAsString(params, "buildingTag", "").toLowerCase();
                    targetEntityStr = inferEntityFromBuildingTag(tag);
                }

                // 根据实体 ID 获取对应的刷怪蛋并放入输入槽展示
                if (!targetEntityStr.isEmpty()) {
                    spawnEggStack = resolveSpawnEgg(targetEntityStr);
                    if (!spawnEggStack.isEmpty()) {
                        rawInputs.add(new AbstractMillCraftingType.IngredientWithCount(Ingredient.of(spawnEggStack), 1));
                    }
                }

                // 3. 解析食物/工具输入（支持 "foodItems" 数组、"foodItem" 字符串及 "shearing" 剪刀）
                if (params.has("foodItems")) {
                    parseFoodItems(params.get("foodItems"), rawInputs);
                } else if (params.has("foodItem")) {
                    parseFoodItems(params.get("foodItem"), rawInputs);
                }

                if ("shearing".equals(handlerId)) {
                    rawInputs.add(new AbstractMillCraftingType.IngredientWithCount(Ingredient.of(Items.SHEARS), 1));
                }

                // 4. 解析产物 (支持 "bonusItems" 数组、"loot" 数组及 "harvestItem")
                if (params.has("bonusItems")) {
                    parseBonusItems(params.get("bonusItems"), outputs);
                }
                if (params.has("loot")) {
                    parseLootItems(params.get("loot"), outputs);
                }
                if (params.has("harvestItem")) {
                    String harvestItemId = GsonHelper.getAsString(params, "harvestItem", "");
                    int count = GsonHelper.getAsInt(params, "harvestCount", 1);
                    Item item = ItemHelper.resolve(harvestItemId);
                    if (item != null && item != Items.AIR) {
                        outputs.add(new ItemStack(item, count));
                    }
                }

                // 5. 提取数量门槛与限制
                if (params.has("maxCount")) maxCount = GsonHelper.getAsInt(params, "maxCount");
                if (params.has("minCount")) minCount = GsonHelper.getAsInt(params, "minCount");
                if (params.has("limit") && maxCount == null) maxCount = GsonHelper.getAsInt(params, "limit");
            }

            // 6. 兜底产物提取：若未显式声明任何 bonusItems/loot，则根据实体类型与行为生成默认常规掉落物/幼体
            if (outputs.isEmpty()) {
                populateDefaultOutputs(handlerId, targetEntityStr, spawnEggStack, outputs);
            }

            // 7. 基类通用输出提取兜底
            if (outputs.isEmpty()) {
                parseOutputs(type, params, outputs);
            }

            if (rawInputs.isEmpty() && outputs.isEmpty()) {
                return null;
            }

            List<IMillRecipe.IngredientWithCount> inputs = rawInputs.stream()
                    .map(raw -> new IMillRecipe.IngredientWithCount(raw.ingredient(), raw.count()))
                    .collect(Collectors.toList());

            String goalKey = type.id() != null ? type.id().getPath() : "unknown";

            return new GoalEntityInteractionRecipe(
                    type.id(), goalKey, handlerId,
                    inputs, outputs, catalysts, villagers,
                    targetEntityStr, spawnEggStack, maxCount, minCount, handlerId
            );
        }

        // region 内部解析辅助逻辑
        /** 根据 buildingTag 推断实体 ID（适配 shearing 等仅包含 buildingTag 的 JSON） */
        private static String inferEntityFromBuildingTag(String tag) {
            if (tag.contains("pig")) return "minecraft:pig";
            if (tag.contains("cattle") || tag.contains("cow")) return "minecraft:cow";
            if (tag.contains("sheep")) return "minecraft:sheep";
            if (tag.contains("chicken")) return "minecraft:chicken";
            if (tag.contains("squid")) return "minecraft:squid";
            return "";
        }

        /** 解析 foodItems JSON 节点 (支持带 # 前缀的物品标签与普通物品 ID) */
        private static void parseFoodItems(JsonElement elem, List<AbstractMillCraftingType.IngredientWithCount> rawInputs) {
            if (elem == null) return;
            if (elem.isJsonArray()) {
                JsonArray array = elem.getAsJsonArray();
                for (JsonElement itemElem : array) {
                    addFoodIngredient(itemElem.getAsString(), rawInputs);
                }
            } else if (elem.isJsonPrimitive()) {
                addFoodIngredient(elem.getAsString(), rawInputs);
            }
        }

        /** 尝试将字符串解析为 Tag Ingredient 或普通 Item Ingredient */
        private static void addFoodIngredient(String foodStr, List<AbstractMillCraftingType.IngredientWithCount> rawInputs) {
            if (foodStr == null || foodStr.isEmpty()) return;
            if (foodStr.startsWith("#")) {
                ResourceLocation tagLoc = ResourceLocation.tryParse(foodStr.substring(1));
                if (tagLoc != null) {
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
                    rawInputs.add(new AbstractMillCraftingType.IngredientWithCount(Ingredient.of(tagKey), 1));
                    return;
                }
            }
            Item foodItem = ItemHelper.resolve(foodStr);
            if (foodItem != null && foodItem != Items.AIR) {
                rawInputs.add(new AbstractMillCraftingType.IngredientWithCount(Ingredient.of(foodItem), 1));
            }
        }

        /** 解析 bonusItems 数组 */
        private static void parseBonusItems(JsonElement elem, List<ItemStack> outputs) {
            if (elem != null && elem.isJsonArray()) {
                for (JsonElement itemElem : elem.getAsJsonArray()) {
                    if (itemElem.isJsonObject()) {
                        JsonObject obj = itemElem.getAsJsonObject();
                        String itemId = GsonHelper.getAsString(obj, "item", "");
                        int count = GsonHelper.getAsInt(obj, "count", 1);
                        Item item = ItemHelper.resolve(itemId);
                        if (item != null && item != Items.AIR) {
                            outputs.add(new ItemStack(item, count));
                        }
                    }
                }
            }
        }

        /** 解析 loot 数组 */
        private static void parseLootItems(JsonElement elem, List<ItemStack> outputs) {
            if (elem != null && elem.isJsonArray()) {
                for (JsonElement itemElem : elem.getAsJsonArray()) {
                    if (itemElem.isJsonObject()) {
                        JsonObject obj = itemElem.getAsJsonObject();
                        String itemId = GsonHelper.getAsString(obj, JSON_KEY_ITEM, "");
                        int count = GsonHelper.getAsInt(obj, JSON_KEY_COUNT, 1);
                        Item item = ItemHelper.resolve(itemId);
                        if (item != null && item != Items.AIR) {
                            outputs.add(new ItemStack(item, count));
                        }
                    }
                }
            }
        }

        /** 兜底填充：当没有额外声明产物时，根据实体与行为生成原版默认产物 */
        private static void populateDefaultOutputs(String handlerId, String entityId, ItemStack spawnEgg, List<ItemStack> outputs) {
            if ("breeding".equals(handlerId)) {
                if (!spawnEgg.isEmpty()) {
                    outputs.add(spawnEgg.copy());
                }
            } else if ("shearing".equals(handlerId)) {
                outputs.add(new ItemStack(Items.WHITE_WOOL));
            } else if ("slaughter".equals(handlerId)) {
                switch (entityId) {
                    case "minecraft:pig" -> outputs.add(new ItemStack(Items.PORKCHOP));
                    case "minecraft:cow" -> {
                        outputs.add(new ItemStack(Items.BEEF));
                        outputs.add(new ItemStack(Items.LEATHER));
                    }
                    case "minecraft:sheep" -> {
                        outputs.add(new ItemStack(Items.MUTTON));
                        outputs.add(new ItemStack(Items.WHITE_WOOL));
                    }
                    case "minecraft:chicken" -> {
                        outputs.add(new ItemStack(Items.CHICKEN));
                        outputs.add(new ItemStack(Items.FEATHER));
                    }
                    case "minecraft:squid" -> outputs.add(new ItemStack(Items.INK_SAC));
                }
            }
        }

        /** 根据实体注册名查找刷怪蛋 */
        private static ItemStack resolveSpawnEgg(String entityIdStr) {
            try {
                ResourceLocation entityLoc = ResourceLocation.tryParse(entityIdStr);
                if (entityLoc != null) {
                    Optional<EntityType<?>> entityTypeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(entityLoc);
                    if (entityTypeOpt.isPresent()) {
                        SpawnEggItem egg = SpawnEggItem.byId(entityTypeOpt.get());
                        if (egg != null) {
                            return new ItemStack(egg);
                        }
                    }
                }
            } catch (Exception ignored) {}
            return ItemStack.EMPTY;
        }
        // endregion 内部解析辅助逻辑
    }
}