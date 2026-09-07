package com.momos.millenairejeim.jei.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.momos.millenairejeim.helper.MillenaireAPIHelper;
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
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.ItemHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 千年工艺 JEI 配方实体模型。
 * 对应底层 {@link GatheringType} 中 handlerId 为 "crafting" 等加工类的数据结构。
 */
public class MillCraftingRecipe {

    // region 常量定义
    /** 默认 Goal 标识字符串。用于 {@link #getGoalKey()} 回退。*/
    private static final String DEFAULT_GOAL_KEY = "unknown";

    /** 默认工艺类型标识字符串。用于 {@link #getCraftingType()} 回退，对应默认 {@link GatheringType#handlerId()}。*/
    private static final String DEFAULT_CRAFTING_TYPE = "crafting";

    /** JSON 解析字段 key：输入物品数组名，对应 {@link JsonObject} 中的 {@code "inputs"}。*/
    private static final String JSON_KEY_INPUTS = "inputs";

    /** JSON 解析字段 key：输出物品数组名，对应 {@link JsonObject} 中的 {@code "outputs"}。*/
    private static final String JSON_KEY_OUTPUTS = "outputs";

    /** JSON 解析字段 key：物品标识 ID 键名，对应 {@link JsonObject} 中的 {@code "item"}。*/
    private static final String JSON_KEY_ITEM = "item";

    /** JSON 解析字段 key：物品数量键名，对应 {@link JsonObject} 中的 {@code "count"}。*/
    private static final String JSON_KEY_COUNT = "count";

    /** 物品 Tag 前缀标识符。用于解析如 {@code "#c:ingots/iron"} 类型的标签输入。*/
    private static final String TAG_PREFIX = "#";
    // endregion 常量定义

    // region 字段定义
    private final ResourceLocation id;

    /**
     * 关联 Goal 的标识 Key（例如 "craftudon"）。
     * 提取自 {@link GatheringType#id()}。
     */
    private final String goalKey;

    /**
     * 工艺/加工类型标识（对应 {@link GatheringType#handlerId()}，例如 "crafting", "cooking", "baking", "smelting"）。
     */
    private final String craftingType;

    private final List<IngredientWithCount> inputs;
    private final List<ItemStack> outputs;

    /**
     * 关联的村民类型列表。
     * 保存具备执行此工艺能力的千年村民实体配置 {@link VillagerType}。
     */
    private final List<VillagerType> villagerTypes;
    // endregion 字段定义

    // region 内部记录类型
    /**
     * 带所需数量标记的 {@link Ingredient} 封装。
     */
    public record IngredientWithCount(Ingredient ingredient, int count) {
    }
    // endregion 内部记录类型

    /**
     * 包含 Goal 名称、工艺类型与关联村民类型的完整构造函数。
     *
     * @param id 配方标识 {@link ResourceLocation}
     * @param goalKey Goal 标识 Key（对应 {@link GatheringType#id()} 的 path）
     * @param craftingType 工艺类型标识（对应 {@link GatheringType#handlerId()}）
     * @param inputs 输入物品列表 {@link IngredientWithCount}
     * @param outputs 输出物品列表 {@link ItemStack}
     * @param villagerTypes 负责执行该配方的村民类型 {@link VillagerType} 列表
     */
    public MillCraftingRecipe(ResourceLocation id, String goalKey, String craftingType, List<IngredientWithCount> inputs, List<ItemStack> outputs, List<VillagerType> villagerTypes) {
        this.id = id;
        this.goalKey = goalKey != null ? goalKey : DEFAULT_GOAL_KEY;
        this.craftingType = craftingType != null ? craftingType : DEFAULT_CRAFTING_TYPE;
        this.inputs = inputs;
        this.outputs = outputs;
        this.villagerTypes = villagerTypes != null ? villagerTypes : Collections.emptyList();
    }
    // endregion 构造函数

    // region Getter 与数据关联 API
    public ResourceLocation getId() {
        return id;
    }

    /**
     * 获取关联 Goal 的 Key/名称字符串。
     *
     * @return Goal 名称，例如 "craftudon"
     */
    public String getGoalKey() {
        return goalKey;
    }

    /**
     * 获取当前配方的工艺类型标识（对应 {@link GatheringType#handlerId()}）。
     *
     * @return 工艺类型字符串，例如 "crafting", "cooking", "baking", "smelting"
     */
    public String getCraftingType() {
        return craftingType;
    }

    public List<IngredientWithCount> getInputs() {
        return inputs;
    }

    public List<ItemStack> getOutputs() {
        return outputs;
    }

    /**
     * 获取支持制作当前配方的村民类型列表。
     * @return 关联的 {@link VillagerType} 列表
     */
    public List<VillagerType> getVillagerTypes() {
        return villagerTypes;
    }

    /**
     * 获取制作当前配方关联的所有文化名称字符串集合（自动去重）。
     * 通过调用 {@link VillagerType#culture()} 获取。
     *
     * @return 文化标识名称列表 {@link List}
     */
    public List<String> getCultureNames() {
        if (villagerTypes.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> cultures = new ArrayList<>();
        for (VillagerType vt : villagerTypes) {
            if (vt != null && vt.culture() != null) {
                String cultureStr = vt.culture().toString();
                if (!cultures.contains(cultureStr)) {
                    cultures.add(cultureStr);
                }
            }
        }
        return cultures;
    }

    /**
     * 快捷调用 {@link MillenaireAPIHelper#MappingVillagerBuilding(VillagerType)}，
     * 获取与当前配方关联的所有村民居住/工作建筑图纸集合。
     * @return 汇总去重后的建筑资源位置 {@link BuildingPlanSet} 列表
     */
    public List<BuildingPlanSet> getAssociatedBuildingIds() {
        if (villagerTypes.isEmpty()) {
            return Collections.emptyList();
        }
        List<BuildingPlanSet> buildings = new ArrayList<>();
        for (VillagerType villagerType : villagerTypes) {
            for (BuildingPlanSet bps : MillenaireAPIHelper.MappingVillagerBuilding(villagerType)) {
                if (!buildings.contains(bps)) {
                    buildings.add(bps);
                }
            }
        }
        return buildings;
    }
    // endregion Getter 与数据关联 API

    // region 配方解析工厂方法
    /**
     * 从 {@link GatheringType} 以及绑定的村民类型列表中解析并构造 JEI 配方实例。
     *
     * @param type 千年采集/加工类型实体 {@link GatheringType}
     * @param villagerTypes 关联的村民类型 {@link VillagerType} 列表
     * @return 转换后的 {@link MillCraftingRecipe}，若输入输出均为空则返回 null
     */
    public static MillCraftingRecipe parse(GatheringType type, List<VillagerType> villagerTypes) {
        JsonObject params = type.handlerParams();
        if (params == null) {
            return null;
        }

        List<IngredientWithCount> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();

        // 解析 handlerParams 中的 inputs 数组
        if (params.has(JSON_KEY_INPUTS)) {
            for (JsonElement elem : params.getAsJsonArray(JSON_KEY_INPUTS)) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                String itemId = GsonHelper.getAsString(obj, JSON_KEY_ITEM);
                int count = GsonHelper.getAsInt(obj, JSON_KEY_COUNT, 1);

                if (itemId.startsWith(TAG_PREFIX)) {
                    // 处理标签输入（如 #c:ingots/iron）
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
                    // 处理具体物品输入
                    Item item = ItemHelper.resolve(itemId);
                    if (item != null && item != Items.AIR) {
                        inputs.add(new IngredientWithCount(Ingredient.of(item), count));
                    }
                }
            }
        }

        // 解析 handlerParams 中的 outputs 数组
        if (params.has(JSON_KEY_OUTPUTS)) {
            for (JsonElement elem : params.getAsJsonArray(JSON_KEY_OUTPUTS)) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                String itemId = GsonHelper.getAsString(obj, JSON_KEY_ITEM);
                int count = GsonHelper.getAsInt(obj, JSON_KEY_COUNT, 1);
                Item item = ItemHelper.resolve(itemId);
                if (item != null && item != Items.AIR) {
                    outputs.add(new ItemStack(item, count));
                }
            }
        }

        if (inputs.isEmpty() && outputs.isEmpty()) {
            return null;
        }

        // 提取 GatheringType 的 ID 路径（例如 "craftudon"）作为 Goal 名称
        String goalKey = type.id() != null ? type.id().getPath() : DEFAULT_GOAL_KEY;

        // 提取 GatheringType 中定义的 handlerId（如 "crafting", "cooking", "baking", "smelting"）作为工艺类型标识
        String craftingType = type.handlerId();

        return new MillCraftingRecipe(type.id(), goalKey, craftingType, inputs, outputs, villagerTypes);
    }
    // endregion 配方解析工厂方法
}