package com.momos.millenairejeim.jei.crafting.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipeManagerPlugin;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.MillCraftingBaseRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.ItemHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对应 handler: "harvesting", "fruit_harvesting", "cocoa_harvesting", "paddy_harvesting" 等收割/采摘/收获配方实体。
 * 包含作物目标方块、成熟度属性、显式收获物、harvestItems 多概率检定及灌溉额外产物等扩展数据。
 */
public class GoalHarvestRecipe extends MillCraftingBaseRecipe {

    private final String targetBlock;
    private final String soilSubtype;
    private final String ageProperty;
    private final Integer ripeAge;
    private final Integer resetAge;
    private final ItemStack irrigationBonusCrop;

    public GoalHarvestRecipe(ResourceLocation id, String goalKey, String handlerId,
                             List<IngredientWithCount> inputs, List<ItemStack> outputs,
                             List<ItemStack> catalysts, List<VillagerType> villagerTypes,
                             String targetBlock, String soilSubtype, String ageProperty,
                             Integer ripeAge, Integer resetAge, ItemStack irrigationBonusCrop) {
        super(id, goalKey, handlerId, inputs, outputs, catalysts, villagerTypes);
        this.targetBlock = targetBlock;
        this.soilSubtype = soilSubtype;
        this.ageProperty = ageProperty;
        this.ripeAge = ripeAge;
        this.resetAge = resetAge;
        this.irrigationBonusCrop = irrigationBonusCrop != null ? irrigationBonusCrop : ItemStack.EMPTY;
    }

    /** 获取目标作物/树叶方块 ResourceLocation 字符串 (如 "minecraft:carrots", "millenaire:apple_tree_leaves") */
    public String getTargetBlock() { return targetBlock; }

    /** 获取土壤类型 (如 "carrot", "cacao", "rice") */
    public String getSoilSubtype() { return soilSubtype; }

    /** 获取年龄属性字段名 (如 "age") */
    public String getAgeProperty() { return ageProperty; }

    /** 获取成熟年龄上限 (果树采摘专用) */
    public Integer getRipeAge() { return ripeAge; }

    /** 获取采摘后重置年龄 (果树采摘专用) */
    public Integer getResetAge() { return resetAge; }

    /** 获取村庄灌溉满足时的额外奖励作物 {@link ItemStack} */
    public ItemStack getIrrigationBonusCrop() { return irrigationBonusCrop; }

    /** 是否具备灌溉额外奖励 */
    public boolean hasIrrigationBonus() {
        return !irrigationBonusCrop.isEmpty();
    }

    private record HarvestRoll(int count, int chance) {}

    /**
     * 收割类配方静态解析器实现。
     */
    public static class Parser extends AbstractMillCraftingType {

        /**
         * 校验传入的 handlerId 是否属于收割类 Handler 集合 {@link MillCraftingRecipeManagerPlugin#HarvestTypeSet}。
         */
        @Override
        public boolean supports(String handlerId, GatheringType type) {
            return handlerId != null && MillCraftingRecipeManagerPlugin.HarvestTypeSet.contains(handlerId.toLowerCase());
        }

        /**
         * 解析 {@link GatheringType} 中 handlerParams 的收割参数并生成 {@link GoalHarvestRecipe} 实例。
         */
        @Override
        public IMillCraftingRecipe parse(GatheringType type, List<VillagerType> villagers) {
            if (type == null) return null;

            JsonObject params = type.handlerParams();
            List<AbstractMillCraftingType.IngredientWithCount> rawInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            List<ItemStack> catalysts = new ArrayList<>();
            Map<Item, List<Component>> pendingTooltips = new LinkedHashMap<>();

            // 1. 基础输入/工具解析（兼容常规 inputs 声明）
            parseInputs(params, rawInputs);

            String targetBlockStr = "";
            String soilSubtypeStr = "";
            String agePropertyStr = "";
            Integer ripeAgeVal = null;
            Integer resetAgeVal = null;
            ItemStack irrigationBonusStack = ItemStack.EMPTY;
            boolean hasHarvestItemsEntries = false;

            String handlerId = type.handlerId() != null ? type.handlerId() : "harvesting";

            if (params != null) {
                // 2. 解析目标方块 targetBlock (如 "minecraft:carrots", "millenaire:apple_tree_leaves")
                if (params.has("targetBlock")) {
                    targetBlockStr = GsonHelper.getAsString(params, "targetBlock", "");
                    if (!targetBlockStr.isEmpty()) {
                        Item blockItem = ItemHelper.resolve(targetBlockStr);
                        if (blockItem != null && blockItem != Items.AIR) {
                            rawInputs.add(new AbstractMillCraftingType.IngredientWithCount(Ingredient.of(blockItem), 1));
                        }
                    }
                }

                // 3. 解析显式单一产物 harvestItem & harvestCount (果树或花卉采摘)
                if (params.has("harvestItem")) {
                    String harvestItemId = GsonHelper.getAsString(params, "harvestItem", "");
                    int harvestCount = GsonHelper.getAsInt(params, "harvestCount", 1);
                    Item item = ItemHelper.resolve(harvestItemId);
                    if (item != null && item != Items.AIR && harvestCount > 0) {
                        outputs.add(new ItemStack(item, harvestCount));
                    }
                }

                // 3b. 解析 harvestItems 多项/多概率掉落数组（跳过 chance <= 0 的除草/清理 Goal）
                if (params.has("harvestItems") && params.get("harvestItems").isJsonArray()) {
                    JsonArray harvestArray = params.getAsJsonArray("harvestItems");
                    Map<Item, List<HarvestRoll>> groupedRolls = new LinkedHashMap<>();
                    for (JsonElement elem : harvestArray) {
                        if (!elem.isJsonObject()) continue;
                        hasHarvestItemsEntries = true;
                        JsonObject obj = elem.getAsJsonObject();
                        int chance = GsonHelper.getAsInt(obj, "chance", 100);
                        int count = GsonHelper.getAsInt(obj, JSON_KEY_COUNT, 1);
                        if (chance <= 0 || count <= 0) {
                            continue;
                        }
                        String itemId = GsonHelper.getAsString(obj, JSON_KEY_ITEM, "");
                        if (itemId.isEmpty()) {
                            itemId = GsonHelper.getAsString(obj, "id", "");
                        }
                        Item item = ItemHelper.resolve(itemId);
                        if (item != null && item != Items.AIR) {
                            groupedRolls.computeIfAbsent(item, k -> new ArrayList<>())
                                    .add(new HarvestRoll(count, chance));
                        }
                    }

                    for (Map.Entry<Item, List<HarvestRoll>> entry : groupedRolls.entrySet()) {
                        Item item = entry.getKey();
                        List<HarvestRoll> rolls = entry.getValue();
                        int guaranteedSum = rolls.stream()
                                .filter(r -> r.chance() >= 100)
                                .mapToInt(HarvestRoll::count)
                                .sum();
                        int displayCount = guaranteedSum > 0 ? guaranteedSum : Math.max(1, rolls.get(0).count());
                        if (!containsItem(outputs, item)) {
                            outputs.add(new ItemStack(item, displayCount));
                        }
                        if (rolls.size() == 1) {
                            int singleChance = rolls.get(0).chance();
                            if (singleChance < 100) {
                                pendingTooltips.computeIfAbsent(item, k -> new ArrayList<>())
                                        .add(Component.translatableWithFallback(
                                                MillenaireJeimKeys.KEY_TOOLTIP_DROP_CHANCE,
                                                MillenaireJeimKeys.FALLBACK_TOOLTIP_DROP_CHANCE,
                                                singleChance
                                        ));
                            }
                        } else {
                            String rollsSummary = rolls.stream()
                                    .map(r -> r.count() + "x(" + r.chance() + "%)")
                                    .collect(Collectors.joining(", "));
                            pendingTooltips.computeIfAbsent(item, k -> new ArrayList<>())
                                    .add(Component.translatableWithFallback(
                                            MillenaireJeimKeys.KEY_TOOLTIP_DROP_ROLLS,
                                            MillenaireJeimKeys.FALLBACK_TOOLTIP_DROP_ROLLS,
                                            rollsSummary
                                    ));
                        }
                    }
                }

                // 4. 解析灌溉额外产物 irrigationBonusCrop（独立存放在专门的灌溉奖励字段，不在主产物槽位重复显示）
                if (params.has("irrigationBonusCrop")) {
                    String bonusItemId = GsonHelper.getAsString(params, "irrigationBonusCrop", "");
                    Item bonusItem = ItemHelper.resolve(bonusItemId);
                    if (bonusItem != null && bonusItem != Items.AIR) {
                        irrigationBonusStack = new ItemStack(bonusItem, 1);
                    }
                }

                // 5. 提取元数据 (用于后续扩展或 Tooltip 渲染)
                if (params.has("soilSubtype")) {
                    soilSubtypeStr = GsonHelper.getAsString(params, "soilSubtype", "");
                }
                if (params.has("ageProperty")) {
                    agePropertyStr = GsonHelper.getAsString(params, "ageProperty", "");
                }
                if (params.has("ripeAge")) {
                    ripeAgeVal = GsonHelper.getAsInt(params, "ripeAge");
                }
                if (params.has("resetAge")) {
                    resetAgeVal = GsonHelper.getAsInt(params, "resetAge");
                }
            }

            // 5b. 针对可可豆硬编码 Handler (CocoaHarvestingHandler) 的兜底产物与灌溉奖励
            if ("cocoa_harvesting".equalsIgnoreCase(handlerId)) {
                if (outputs.isEmpty()) {
                    outputs.add(new ItemStack(Items.COCOA_BEANS, 2));
                }
                if (irrigationBonusStack.isEmpty()) {
                    irrigationBonusStack = new ItemStack(Items.COCOA_BEANS, 1);
                }
            }

            // 5c. 针对水田收割 (PaddyHarvestingHandler) 的作物输入补全
            if (rawInputs.isEmpty() && ("paddy_harvesting".equalsIgnoreCase(handlerId)
                    || "rice".equalsIgnoreCase(soilSubtypeStr))) {
                Item ricePaddy = ItemHelper.resolve("millenaire:rice_paddy");
                if (ricePaddy != null && ricePaddy != Items.AIR) {
                    rawInputs.add(new AbstractMillCraftingType.IngredientWithCount(Ingredient.of(ricePaddy), 1));
                }
            }

            // 若 JSON 显式声明了 harvestItems（例如除草 Goal chance:0）导致过滤后无任何产出，不回退为 heldItems
            if (hasHarvestItemsEntries && outputs.isEmpty() && irrigationBonusStack.isEmpty()) {
                return null;
            }

            // 6. 兜底输出解析：若未显式指定 harvestItem / harvestItems，调用基类通用输出提取
            if (outputs.isEmpty()) {
                parseOutputs(type, params, outputs);
            }

            // 7. 兜底输入解析：若无显式 inputs 但存在 targetBlock，确保作为输入项
            if (rawInputs.isEmpty() && !targetBlockStr.isEmpty()) {
                Item blockItem = ItemHelper.resolve(targetBlockStr);
                if (blockItem != null && blockItem != Items.AIR) {
                    rawInputs.add(new AbstractMillCraftingType.IngredientWithCount(Ingredient.of(blockItem), 1));
                }
            }

            if (rawInputs.isEmpty() && outputs.isEmpty() && irrigationBonusStack.isEmpty()) {
                return null;
            }

            List<IMillCraftingRecipe.IngredientWithCount> inputs = rawInputs.stream()
                    .map(raw -> new IMillCraftingRecipe.IngredientWithCount(raw.ingredient(), raw.count()))
                    .collect(Collectors.toList());

            String goalKey = type.id() != null ? type.id().getPath() : "unknown";

            GoalHarvestRecipe recipe = new GoalHarvestRecipe(
                    type.id(), goalKey, handlerId,
                    inputs, outputs, catalysts, villagers,
                    targetBlockStr, soilSubtypeStr, agePropertyStr,
                    ripeAgeVal, resetAgeVal, irrigationBonusStack
            );
            pendingTooltips.forEach(recipe::addOutputTooltips);
            return recipe;
        }
    }
}
