package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.util.MMLog;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.gathering.GatheringGoal;
import org.millenaire.goal.gathering.GatheringType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 千年工艺与烹饪配方提取工厂。
 * 从 {@link GoalRegistry} 中提取包含合成、烹饪、冶炼等转换类 Handlers 的 {@link GatheringGoal}。
 */
public class MillCraftingRecipeMaker {

    // -----------------------------------------------------------------------------------
    // [新注释] 移除原 SLF4J Logger 静态定义，全面转用 {@link MMLog} 工具类以实现三语（中/英/法）控制台日志输出。
    // -----------------------------------------------------------------------------------

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
            // [新注释] 使用 MMLog.warn 输出三语警告日志
            MMLog.warn(
                    "[Millenaire-JEI] GoalRegistry 实例为空！无法加载千年工艺配方，请检查 MillenaireJeiPlugin.setGoalRegistry() 的赋值时机。",
                    "[Millenaire-JEI] GoalRegistry instance is null! Unable to load Millénaire crafting recipes. Please check the timing of MillenaireJeiPlugin.setGoalRegistry().",
                    "[Millenaire-JEI] L'instance GoalRegistry est nulle ! Impossible de charger les recettes de fabrication Millénaire, veuillez vérifier le moment de l'affectation de MillenaireJeiPlugin.setGoalRegistry()."
            );
            return recipes;
        }
        // 使用容错后(真正激活)的 activeRegistry 提取目标 {@link GatheringGoal} 列表
        List<GatheringGoal> gatheringGoals = activeRegistry.getGatheringGoals();

        int totalGoals = gatheringGoals != null ? gatheringGoals.size() : 0;
        MMLog.info(
                "[Millenaire-JEI] 开始解析千年工艺配方，获取到的 GatheringGoal 总数: {}",
                "[Millenaire-JEI] Starting parsing Millénaire crafting recipes. Total GatheringGoals fetched: {}",
                "[Millenaire-JEI] Début de l'analyse des recettes de fabrication Millénaire, nombre total de GatheringGoal obtenus : {}",
                totalGoals
        );

        if (gatheringGoals == null || gatheringGoals.isEmpty()) {
            MMLog.warn(
                    "[Millenaire-JEI] GatheringGoal 列表为空，未找到任何采集与加工 Goal 数据！",
                    "[Millenaire-JEI] GatheringGoal list is empty, no gathering or processing Goal data found!",
                    "[Millenaire-JEI] La liste GatheringGoal est vide, aucune donnée Goal de collecte ou de fabrication trouvée !"
            );
            return recipes;
        }

        // 构建 Goal ID / GatheringType ID -> VillagerType 的反向映射表，用于关联配方与制作者
        Map<ResourceLocation, List<VillagerType>> goalToVillagersMap = buildGoalToVillagerMap();

        int matchedCount = 0;
        int parsedCount = 0;

        for (GatheringGoal goal : gatheringGoals) {
            GatheringType type = goal.getGatheringType();
            if (type != null && type.handlerId() != null) {
                String handlerId = type.handlerId().toLowerCase();
                // 只要属于合成/烹饪类 Handler 均进行解析
                if (CRAFTING_HANDLER_IDS.contains(handlerId)) {
                    matchedCount++;

                    // 检索当前配方类型绑定的所有村民类型
                    List<VillagerType> matchedVillagers = goalToVillagersMap.getOrDefault(type.id(), List.of());

                    MillCraftingRecipe recipe = MillCraftingRecipe.parse(type, matchedVillagers);
                    if (recipe != null) {
                        recipes.add(recipe);
                        parsedCount++;
                        MMLog.debug(
                                "[Millenaire-JEI] 成功解析配方: ID={}, Handler={}, 关联村民数={}",
                                "[Millenaire-JEI] Successfully parsed recipe: ID={}, Handler={}, Associated villagers count={}",
                                "[Millenaire-JEI] Recette analysée avec succès : ID={}, Handler={}, Nombre de villageois associés={}",
                                type.id(), handlerId, matchedVillagers.size()
                        );
                    } else {
                        MMLog.warn(
                                "[Millenaire-JEI] 配方解析失败 (parse 返回 null): ID={}, Handler={}, handlerParams={}",
                                "[Millenaire-JEI] Failed to parse recipe (parse returned null): ID={}, Handler={}, handlerParams={}",
                                "[Millenaire-JEI] Échec de l'analyse de la recette (parse a renvoyé null) : ID={}, Handler={}, handlerParams={}",
                                type.id(), handlerId, type.handlerParams()
                        );
                    }
                }
            }
        }

        MMLog.info(
                "[Millenaire-JEI] 配方提取完成。匹配目标 Handler: {} 个，成功解析配方: {} 个。",
                "[Millenaire-JEI] Recipe extraction complete. Matched target Handlers: {}, successfully parsed recipes: {}.",
                "[Millenaire-JEI] Extraction des recettes terminée. Handlers cibles correspondants : {}, recettes analysées avec succès : {}.",
                matchedCount, parsedCount
        );

        if (!recipes.isEmpty()) {
            MMLog.info(
                    "[Millenaire-JEI] 已向 JEI 成功注册 {} 个千年工艺/烹饪配方！",
                    "[Millenaire-JEI] Successfully registered {} Millénaire crafting/cooking recipes to JEI!",
                    "[Millenaire-JEI] Enregistrement réussi de {} recettes de fabrication/cuisine Millénaire dans JEI !",
                    recipes.size()
            );
        } else {
            MMLog.warn(
                    "[Millenaire-JEI] 最终生成的配方列表为空，JEI 将自动隐藏该 Category 页签！",
                    "[Millenaire-JEI] The final generated recipe list is empty. JEI will automatically hide this Category tab!",
                    "[Millenaire-JEI] La liste finale des recettes générées est vide, JEI masquera automatiquement cet onglet de catégorie !"
            );
        }
        return recipes;
    }

    /**
     * 构建的目标 Goal / GatheringType ID 到对应村民类型列表的建立关系字典。
     *
     * @return 映射字典 {@link Map}
     */
    private static Map<ResourceLocation, List<VillagerType>> buildGoalToVillagerMap() {
        Map<ResourceLocation, List<VillagerType>> map = new HashMap<>();
        Map<ResourceLocation, VillagerType> allVillagerType = MillenaireAPIHelper.getAllVillagerType();

        if (allVillagerType == null || allVillagerType.isEmpty()) {
            return map;
        }

        for (VillagerType vt : allVillagerType.values()) {
            if (vt == null || vt.goals() == null) continue;

            List<ResourceLocation> goals = vt.goals();
            for (ResourceLocation goalId : goals) {
                if (goalId == null) continue;

                // 直接根据 Goal ID 关联村民
                map.computeIfAbsent(goalId, k -> new ArrayList<>());
                if (!map.get(goalId).contains(vt)) {
                    map.get(goalId).add(vt);
                }

                // 若该 Goal 为 GatheringGoal，同步将其 GatheringType 的 ID 绑定对应村民
                VillagerGoal g = MillenaireAPIHelper.getGoal(goalId);
                if (g instanceof GatheringGoal gatheringGoal) {
                    GatheringType gType = gatheringGoal.getGatheringType();
                    if (gType != null && gType.id() != null) {
                        map.computeIfAbsent(gType.id(), k -> new ArrayList<>());
                        if (!map.get(gType.id()).contains(vt)) {
                            map.get(gType.id()).add(vt);
                        }
                    }
                }
            }
        }
        return map;
    }
}