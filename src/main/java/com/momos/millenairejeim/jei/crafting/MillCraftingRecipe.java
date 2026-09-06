package com.momos.millenairejeim.jei.crafting;

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
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.ItemHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 千年工艺 JEI 配方实体模型。
 * 对应底层 {@link GatheringType} 中 handlerId 为 "crafting" 的数据结构。
 */
public class MillCraftingRecipe {
    private final ResourceLocation id;
    private final List<IngredientWithCount> inputs;
    private final List<ItemStack> outputs;

    /**
     * 带所需数量标记的 {@link Ingredient} 封装。
     */
    public record IngredientWithCount(Ingredient ingredient, int count) {
    }

    public MillCraftingRecipe(ResourceLocation id, List<IngredientWithCount> inputs, List<ItemStack> outputs) {
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public ResourceLocation getId() {
        return id;
    }

    public List<IngredientWithCount> getInputs() {
        return inputs;
    }

    public List<ItemStack> getOutputs() {
        return outputs;
    }


    /**
     * 从 {@link GatheringType} 中安全解析并构造 JEI 配方实例。
     *
     * @param type 千年采集/加工类型实体
     * @return 转换后的 {@link MillCraftingRecipe}，若输入输出均为空则返回 null
     */
    public static MillCraftingRecipe parse(GatheringType type) {
        JsonObject params = type.handlerParams();
        if (params == null) {
            return null;
        }

        List<IngredientWithCount> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();

        // 解析 handlerParams 中的 inputs 数组
        if (params.has("inputs")) {
            for (JsonElement elem : params.getAsJsonArray("inputs")) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                String itemId = GsonHelper.getAsString(obj, "item");
                int count = GsonHelper.getAsInt(obj, "count", 1);

                if (itemId.startsWith("#")) {
                    // 处理标签输入（如 #c:ingots/iron）
                    ResourceLocation tagId = ResourceLocation.tryParse(itemId.substring(1));
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
                    // 处理具体物品输入
                    Item item = ItemHelper.resolve(itemId);
                    if (item != null && item != Items.AIR) {
                        inputs.add(new IngredientWithCount(Ingredient.of(item), count));
                    }
                }
            }
        }

        // 解析 handlerParams 中的 outputs 数组
        if (params.has("outputs")) {
            for (JsonElement elem : params.getAsJsonArray("outputs")) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                String itemId = GsonHelper.getAsString(obj, "item");
                int count = GsonHelper.getAsInt(obj, "count", 1);
                Item item = ItemHelper.resolve(itemId);
                if (item != null && item != Items.AIR) {
                    outputs.add(new ItemStack(item, count));
                }
            }
        }

        if (inputs.isEmpty() && outputs.isEmpty()) {
            return null;
        }

        return new MillCraftingRecipe(type.id(), inputs, outputs);
    }
}