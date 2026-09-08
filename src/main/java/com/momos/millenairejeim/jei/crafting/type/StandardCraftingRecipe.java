package com.momos.millenairejeim.jei.crafting.type;

import com.google.gson.JsonObject;
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipeManagerPlugin;
import com.momos.millenairejeim.jei.crafting.type.base.AbstractMillCraftingType;
import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.MillBaseRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对应 handler: "crafting", "smelting" 的配方实体。
 * 纯数据模型，渲染与 Tooltip 全量交由 {@link com.momos.millenairejeim.jei.crafting.MillCraftingCategory} 负责。
 */
public class StandardCraftingRecipe extends MillBaseRecipe {

    private final boolean isSmelting;

    public StandardCraftingRecipe(ResourceLocation id, String goalKey, String handlerId,
                                  List<IngredientWithCount> inputs, List<ItemStack> outputs,
                                  List<ItemStack> catalysts, List<VillagerType> villagerTypes, boolean isSmelting) {
        super(id, goalKey, handlerId, inputs, outputs, catalysts, villagerTypes);
        this.isSmelting = isSmelting;
    }

    public boolean isSmelting() { return isSmelting; }

    /* =========================================================================================================
     * [新注释] 【解析器结构改动】
     * 移除已废弃的 IMillRecipeParser 接口及其 @Override 标注。
     * 保留继承 {@link AbstractMillCraftingType} 以直接使用抽象父类提供的 JSON 字段解析方法。
     * ========================================================================================================= */
    /** 静态解析器实现 */
    public static class Parser extends AbstractMillCraftingType {

        /* =========================================================================================================
         * [新注释] 【接口解耦说明】
         * 移除 @Override 注解，作为独立匹配与解析工具方法供外部（如 {@link MillRecipeManager}）直接调用。
         * ========================================================================================================= */
        /**
         * 校验传入的 handlerId 是否属于当前标准工艺解析器处理范围。
         *
         * @param handlerId 目标 handler 标识符，如 {@code "crafting"} 或 {@code "smelting"}
         * @param type 底层千年 Goal 采集/加工类型数据实体 {@link GatheringType}
         * @return 若符合处理类型则返回 {@code true}
         */
        public boolean supports(String handlerId, GatheringType type) {
            return handlerId != null && MillCraftingRecipeManagerPlugin.CraftTypeSet.contains(handlerId.toLowerCase());
        }

        /**
         * 解析 {@link GatheringType} 并生成 {@link StandardCraftingRecipe} 实例。
         *
         * @param type 底层千年 Goal 采集/加工类型数据实体 {@link GatheringType}
         * @param villagers 具备执行此动作能力的村民类型 {@link VillagerType} 列表
         * @return 转换后的配方实体 {@link StandardCraftingRecipe}，解析失败或为空时返回 {@code null}
         */
        public StandardCraftingRecipe parse(GatheringType type, List<VillagerType> villagers) {
            if (type == null) return null;

            JsonObject params = type.handlerParams();

            List<AbstractMillCraftingType.IngredientWithCount> rawInputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();

            parseInputs(params, rawInputs);

            List<IMillRecipe.IngredientWithCount> inputs = rawInputs.stream()
                    .map(raw -> new IMillRecipe.IngredientWithCount(raw.ingredient(), raw.count()))
                    .collect(Collectors.toList());

            parseOutputs(type, params, outputs);

            if (inputs.isEmpty() && outputs.isEmpty()) return null;

            String goalKey = type.id() != null ? type.id().getPath() : "unknown";
            String handlerId = type.handlerId();
            boolean isSmelting = "smelting".equalsIgnoreCase(handlerId);

            return new StandardCraftingRecipe(type.id(), goalKey, handlerId, inputs, outputs, List.of(), villagers, isSmelting);
        }
    }
}