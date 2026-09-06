package com.momos.millenairejeim.jei.crafting;

import com.mojang.logging.LogUtils;
import com.momos.millenairejeim.jei.MillenaireJeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.gathering.GatheringGoal;
import org.millenaire.goal.gathering.GatheringType;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 千年工艺与烹饪配方提取工厂。
 * 从 {@link GoalRegistry} 中提取包含合成、烹饪、冶炼等转换类 Handlers 的 {@link GatheringGoal}。
 */
public class MillCraftingRecipeMaker {
    /**
     * 日志记录器，用于追踪 JEI 阶段的配方提取与解析状态。
     */
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 支持转换为 JEI 配方的 Handler ID 集合（涵盖通用合成、烹饪、烘焙与冶炼）
     */
    private static final Set<String> CRAFTING_HANDLER_IDS = Set.of(
            "crafting",
            "cooking",
            "baking",
            "smelting"
    );

    /** 初始化获得条目 */
    public static List<MillCraftingRecipe> initRecipes(GoalRegistry activeRegistry) {
        List<MillCraftingRecipe> recipes = new ArrayList<>();
        if (activeRegistry == null) {
            LOGGER.warn("[Millenaire-JEI] GoalRegistry 实例为空！无法加载千年工艺配方，请检查 MillenaireJeiPlugin.setGoalRegistry() 的赋值时机。");
            return recipes;
        }

        // 【新增注释】：使用容错后的 activeRegistry 提取目标 {@link GatheringGoal} 列表
        List<GatheringGoal> gatheringGoals = activeRegistry.getGatheringGoals();

        int totalGoals = gatheringGoals != null ? gatheringGoals.size() : 0;
        LOGGER.info("[Millenaire-JEI] 开始解析千年工艺配方，获取到的 GatheringGoal 总数: {}", totalGoals);

        if (gatheringGoals == null || gatheringGoals.isEmpty()) {
            LOGGER.warn("[Millenaire-JEI] GatheringGoal 列表为空，未找到任何采集与加工 Goal 数据！");
            return recipes;
        }

        int matchedCount = 0;
        int parsedCount = 0;

        for (GatheringGoal goal : gatheringGoals) {
            GatheringType type = goal.getGatheringType();
            if (type != null && type.handlerId() != null) {
                String handlerId = type.handlerId().toLowerCase();
                // 只要属于合成/烹饪类 Handler 均进行解析
                if (CRAFTING_HANDLER_IDS.contains(handlerId)) {
                    matchedCount++;
                    MillCraftingRecipe recipe = MillCraftingRecipe.parse(type);
                    if (recipe != null) {
                        recipes.add(recipe);
                        parsedCount++;
                        LOGGER.debug("[Millenaire-JEI] 成功解析配方: ID={}, Handler={}", type.id(), handlerId);
                    } else {
                        LOGGER.warn("[Millenaire-JEI] 配方解析失败 (parse 返回 null): ID={}, Handler={}, handlerParams={}",
                                type.id(), handlerId, type.handlerParams());
                    }
                }
            }
        }

        LOGGER.info("[Millenaire-JEI] 配方提取完成。匹配目标 Handler: {} 个，成功解析配方: {} 个。", matchedCount, parsedCount);

        if (!recipes.isEmpty()) {
            LOGGER.info("[Millenaire-JEI] 已向 JEI 成功注册 {} 个千年工艺/烹饪配方！", recipes.size());
        } else {
            LOGGER.warn("[Millenaire-JEI] 最终生成的配方列表为空，JEI 将自动隐藏该 Category 页签！");
        }
        return recipes;
    }
}