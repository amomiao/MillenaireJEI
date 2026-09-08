package com.momos.millenairejeim.jei.crafting.type.base;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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

// region 基类与通用解析逻辑
/**
 * 千年 JEI 配方类型解析抽象基类。
 * 位于 {@code com.momos.millenairejeim.jei.crafting.type} 包下，负责处理 {@link GatheringType} 到 JEI 显示数据的通用提取逻辑。
 */
public abstract class AbstractMillCraftingType {
    protected static final String JSON_KEY_INPUTS = "inputs";
    protected static final String JSON_KEY_OUTPUTS = "outputs";
    protected static final String JSON_KEY_ITEM = "item";
    protected static final String JSON_KEY_COUNT = "count";
    protected static final String TAG_PREFIX = "#";

    /* =========================================================================================================
     * [新注释] 【解析器抽象契约】
     * 取缔 IMillRecipeParser 后，由抽象基类直接声明多态接口方法，供分流工厂类 {@link com.momos.millenairejeim.jei.crafting.MillCraftingRecipe} 调用。
     * ========================================================================================================= */

    /**
     * [新注释] 检查当前解析器是否支持处理指定的 handlerId 及 GatheringType。
     *
     * @param handlerId 底层 Goal 的 handlerId（如 "crafting", "smelting" 等）
     * @param type 底层千年 Goal 采集/加工类型数据实体 {@link GatheringType}
     * @return 若支持解析则返回 {@code true}
     */
    public abstract boolean supports(String handlerId, GatheringType type);

    /**
     * [新注释] 将 {@link GatheringType} 解析转换为具体的 {@link IMillRecipe} 配方实体。
     *
     * @param type 底层千年 Goal 采集/加工类型数据实体 {@link GatheringType}
     * @param villagers 关联具备制作能力的村民类型 {@link VillagerType} 列表
     * @return 解析生成的 {@link IMillRecipe} 实例，若解析失败或无有效产物则返回 {@code null}
     */
    public abstract IMillRecipe parse(GatheringType type, List<VillagerType> villagers);

    /**
     * [新注释] 通用输入项解析逻辑，兼容多种 JSON 节点表达方式。
     *
     * @param params {@link GatheringType#handlerParams()} 的 JSON 对象
     * @param inputs 目标输入列表
     */
    protected void parseInputs(JsonObject params, List<IngredientWithCount> inputs) {
        if (params == null) return;
        String[] inputKeys = {JSON_KEY_INPUTS, "input", "source", "tool", "items"};
        for (String key : inputKeys) {
            if (!params.has(key)) continue;
            JsonElement elem = params.get(key);
            if (elem.isJsonArray()) {
                for (JsonElement itemElem : elem.getAsJsonArray()) {
                    parseSingleInput(itemElem, inputs);
                }
            } else {
                parseSingleInput(elem, inputs);
            }
        }
    }

    /**
     * [新注释] 通用输出项解析逻辑。包含了针对 GatheringType 没有 item() 方法的修复兜底。
     *
     * @param type 原始 {@link GatheringType} 实例
     * @param params {@link GatheringType#handlerParams()} 的 JSON 对象
     * @param outputs 目标输出列表
     */
    protected void parseOutputs(GatheringType type, JsonObject params, List<ItemStack> outputs) {
        if (params != null) {
            String[] outputKeys = {JSON_KEY_OUTPUTS, "output", "result", "products", "drops"};
            for (String key : outputKeys) {
                if (!params.has(key)) continue;
                JsonElement elem = params.get(key);
                if (elem.isJsonArray()) {
                    for (JsonElement itemElem : elem.getAsJsonArray()) {
                        parseSingleOutput(itemElem, outputs);
                    }
                } else {
                    parseSingleOutput(elem, outputs);
                }
            }
        }

        // =========================================================================================================
        // 【修正 GatheringType 编译错误】
        // 反编译显示 GatheringType 不存在 item() 方法。若 JSON 未明示 outputs，此处优先从 handlerParams 提取 "item"，
        // 仍不存在时使用 {@link GatheringType#heldItems()} 的首个元素作为兜底产物。
        // =========================================================================================================
        if (outputs.isEmpty()) {
            if (params != null && params.has(JSON_KEY_ITEM)) {
                String mainItemId = GsonHelper.getAsString(params, JSON_KEY_ITEM, "");
                Item mainDrop = ItemHelper.resolve(mainItemId);
                if (mainDrop != null && mainDrop != Items.AIR) {
                    outputs.add(new ItemStack(mainDrop, 1));
                }
            } else if (type != null && type.heldItems() != null && !type.heldItems().isEmpty()) {
                String firstHeld = type.heldItems().get(0);
                Item mainDrop = ItemHelper.resolve(firstHeld);
                if (mainDrop != null && mainDrop != Items.AIR) {
                    outputs.add(new ItemStack(mainDrop, 1));
                }
            }
        }
    }

    private void parseSingleInput(JsonElement elem, List<IngredientWithCount> inputs) {
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            addIngredientByString(elem.getAsString(), 1, inputs);
        } else if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            String itemId = GsonHelper.getAsString(obj, JSON_KEY_ITEM, "");
            if (itemId.isEmpty()) {
                itemId = GsonHelper.getAsString(obj, "id", "");
            }
            int count = GsonHelper.getAsInt(obj, JSON_KEY_COUNT, 1);
            if (!itemId.isEmpty()) {
                addIngredientByString(itemId, count, inputs);
            }
        }
    }

    private void parseSingleOutput(JsonElement elem, List<ItemStack> outputs) {
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            Item item = ItemHelper.resolve(elem.getAsString());
            if (item != null && item != Items.AIR) {
                outputs.add(new ItemStack(item, 1));
            }
        } else if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            String itemId = GsonHelper.getAsString(obj, JSON_KEY_ITEM, "");
            if (itemId.isEmpty()) {
                itemId = GsonHelper.getAsString(obj, "id", "");
            }
            int count = GsonHelper.getAsInt(obj, JSON_KEY_COUNT, 1);
            Item item = ItemHelper.resolve(itemId);
            if (item != null && item != Items.AIR) {
                outputs.add(new ItemStack(item, count));
            }
        }
    }

    private void addIngredientByString(String itemId, int count, List<IngredientWithCount> inputs) {
        if (itemId.startsWith(TAG_PREFIX)) {
            ResourceLocation tagId = ResourceLocation.tryParse(itemId.substring(TAG_PREFIX.length()));
            if (tagId != null) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
                var holders = BuiltInRegistries.ITEM.getOrCreateTag(tagKey);
                List<ItemStack> tagStacks = new ArrayList<>();
                for (Holder<Item> holder : holders) {
                    tagStacks.add(new ItemStack(holder.value()));
                }
                if (!tagStacks.isEmpty()) {
                    Ingredient ingredient = Ingredient.of(tagStacks.stream());
                    inputs.add(new IngredientWithCount(ingredient, count));
                }
            }
        } else {
            Item item = ItemHelper.resolve(itemId);
            if (item != null && item != Items.AIR) {
                inputs.add(new IngredientWithCount(Ingredient.of(item), count));
            }
        }
    }

    /**
     * 带数量的 Ingredient 包装类。
     */
    public record IngredientWithCount(Ingredient ingredient, int count) {}
}
// endregion 基类与通用解析逻辑