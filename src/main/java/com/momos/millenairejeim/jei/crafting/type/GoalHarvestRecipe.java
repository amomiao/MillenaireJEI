package com.momos.millenairejeim.jei.crafting.type;

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
import net.minecraft.world.item.crafting.Ingredient;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.ItemHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对应 handler: "harvesting", "fruit_harvesting", "cocoa_harvesting" 等收割/采摘/收获配方实体。
 * 包含作物目标方块、成熟度属性、显式收获物及灌溉额外产物等扩展数据。
 */
public class GoalHarvestRecipe extends MillBaseRecipe {

    // region 预留扩展字段 (便于后续 JEI GUI 绘制、Tooltip 展开或 API 联动)
    private final String targetBlock;
    private final String soilSubtype;
    private final String ageProperty;
    private final Integer ripeAge;
    private final Integer resetAge;
    private final ItemStack irrigationBonusCrop;
    // endregion 预留扩展字段

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

    /** 获取土壤类型 (如 "carrot", "cacao") */
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
        public IMillRecipe parse(GatheringType type, List<VillagerType> villagers) {
            if (type == null) return null;

            JsonObject params = type.handlerParams();
            List<AbstractMillCraftingType.IngredientWithCount> rawInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            List<ItemStack> catalysts = new ArrayList<>();

            // 1. 基础输入/工具解析（兼容常规 inputs 声明）
            parseInputs(params, rawInputs);

            String targetBlockStr = "";
            String soilSubtypeStr = "";
            String agePropertyStr = "";
            Integer ripeAgeVal = null;
            Integer resetAgeVal = null;
            ItemStack irrigationBonusStack = ItemStack.EMPTY;

            if (params != null) {
                // 2. 解析目标方块 targetBlock (如 "minecraft:carrots", "millenaire:apple_tree_leaves")
                if (params.has("targetBlock")) {
                    targetBlockStr = GsonHelper.getAsString(params, "targetBlock", "");
                    if (!targetBlockStr.isEmpty()) {
                        Item blockItem = ItemHelper.resolve(targetBlockStr);
                        if (blockItem != null && blockItem != Items.AIR) {
                            // 将目标方块物品作为原料输入展示
                            rawInputs.add(new AbstractMillCraftingType.IngredientWithCount(Ingredient.of(blockItem), 1));
                        }
                    }
                }

                // 3. 解析显式产物 harvestItem & harvestCount (果树或花卉采摘)
                if (params.has("harvestItem")) {
                    String harvestItemId = GsonHelper.getAsString(params, "harvestItem", "");
                    int harvestCount = GsonHelper.getAsInt(params, "harvestCount", 1);
                    Item item = ItemHelper.resolve(harvestItemId);
                    if (item != null && item != Items.AIR) {
                        outputs.add(new ItemStack(item, harvestCount));
                    }
                }

                // 4. 解析灌溉额外产物 irrigationBonusCrop
                if (params.has("irrigationBonusCrop")) {
                    String bonusItemId = GsonHelper.getAsString(params, "irrigationBonusCrop", "");
                    Item bonusItem = ItemHelper.resolve(bonusItemId);
                    if (bonusItem != null && bonusItem != Items.AIR) {
                        irrigationBonusStack = new ItemStack(bonusItem, 1);
                        // 同步放入 outputs 输出列表中展示
                        outputs.add(irrigationBonusStack.copy());
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

            // 6. 兜底输出解析：若未显式指定 harvestItem，调用基类通用输出提取 (或基于 heldItems/掉落物)
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

            if (rawInputs.isEmpty() && outputs.isEmpty()) {
                return null;
            }

            List<IMillRecipe.IngredientWithCount> inputs = rawInputs.stream()
                    .map(raw -> new IMillRecipe.IngredientWithCount(raw.ingredient(), raw.count()))
                    .collect(Collectors.toList());

            String goalKey = type.id() != null ? type.id().getPath() : "unknown";
            String handlerId = type.handlerId();

            return new GoalHarvestRecipe(
                    type.id(), goalKey, handlerId,
                    inputs, outputs, catalysts, villagers,
                    targetBlockStr, soilSubtypeStr, agePropertyStr,
                    ripeAgeVal, resetAgeVal, irrigationBonusStack
            );
        }
    }
}