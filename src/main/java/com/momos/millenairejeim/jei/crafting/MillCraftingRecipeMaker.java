package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
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
 * 千年工艺与加工配方提取工厂。
 * 从 {@link GoalRegistry} 中提取包含合成、烹饪、冶炼、农艺、采矿等转换类 Handlers 的 {@link GatheringGoal}。
 * <p>
 * [新注释] 已重构为四大 JEI 分类分流架构，支持分别获取手工、冶炼、农林采掘与采集捕捞四大类配方。
 */
public class MillCraftingRecipeMaker {
    /** 提取【村民手工与合成】分类配方列表，对接 JEI 的 Crafting Category。*/
    public static List<IMillRecipe> getCraftingRecipes(GoalRegistry activeRegistry) {return getRecipesByHandlers(activeRegistry, MillCraftingRecipeManagerPlugin.CraftTypeSet);}
    /** 提取【农林采掘与生产】分类配方列表，对接 JEI 的 Farming/Mining Category。*/
    public static List<IMillRecipe> getLootTypeSetRecipes(GoalRegistry activeRegistry) {return getRecipesByHandlers(activeRegistry, MillCraftingRecipeManagerPlugin.LootTypeSet);}
    /** 提取【农林收割与采摘】分类配方列表 */
    public static List<IMillRecipe> getHarvestTypeSetRecipes(GoalRegistry activeRegistry) {return getRecipesByHandlers(activeRegistry, MillCraftingRecipeManagerPlugin.HarvestTypeSet);}
    /** 提取【实体交互与牧业】分类配方列表 */
    public static List<IMillRecipe> getOfEntityTypeSetRecipes(GoalRegistry activeRegistry) {return getRecipesByHandlers(activeRegistry, MillCraftingRecipeManagerPlugin.OfEntityTypeSet);}

    /** 初始化获得条目 */
    public static List<IMillRecipe> initRecipes(GoalRegistry activeRegistry) {
        // [新注释] 合并提取手工、冶炼、采掘以及农林收割等全部单页签配方
        List<IMillRecipe> recipes = new ArrayList<>();
        recipes.addAll(getCraftingRecipes(activeRegistry));
        recipes.addAll(getLootTypeSetRecipes(activeRegistry));
        recipes.addAll(getHarvestTypeSetRecipes(activeRegistry));
        recipes.addAll(getOfEntityTypeSetRecipes(activeRegistry));
        return recipes;
    }

    // [新注释] 提供最少 API：按单个 targetHandlerId 过滤提取配方，方便前端 JEI 分类页面接手挂载
    /**
     * 根据指定的 Handler ID 提取对应配方列表。
     * @param activeRegistry 激活的 {@link GoalRegistry} 实例
     * @param targetHandlerId 目标 handler 标识，例如 "crafting" 或 "smelting"
     * @return 解析后的 {@link IMillRecipe} 列表
     */
    public static List<IMillRecipe> getRecipesByHandler(GoalRegistry activeRegistry, String targetHandlerId) {
        if (targetHandlerId == null || targetHandlerId.isBlank()) {
            return List.of();
        }
        return getRecipesByHandlers(activeRegistry, Set.of(targetHandlerId.toLowerCase()));
    }
    // endregion 2. 四大分类 JEI 配方提取对外 API

    // region 3. 核心配方提取与匹配通用逻辑
    // [新注释] 提供最少 API：按 handlerIds 集合 numerically 精准批量提取配方
    /**
     * 根据指定的 Handler ID 集合提取对应的 JEI 配方列表。
     *
     * @param activeRegistry 激活的 {@link GoalRegistry} 实例
     * @param targetHandlerIds 目标 handler 标识集合 {@link Set}
     * @return 解析后的 {@link IMillRecipe} 列表
     */
    public static List<IMillRecipe> getRecipesByHandlers(GoalRegistry activeRegistry, Set<String> targetHandlerIds) {
        List<IMillRecipe> recipes = new ArrayList<>();
        if (activeRegistry == null) {
            // [新注释] 使用 MMLog.warn 输出三语警告日志
            // [醒目新注释] 追加 targetHandlerIds 参数，便于定位目标集合为空时引发注册中断的上下文
            MMLog.warn(
                    "[Millenaire-JEI] GoalRegistry 实例为空！无法加载千年工艺配方，目标 Handlers: {}，请检查 MillenaireJeiPlugin.setGoalRegistry() 的赋值时机。",
                    "[Millenaire-JEI] GoalRegistry instance is null! Unable to load Millénaire crafting recipes for target Handlers: {}. Please check the timing of MillenaireJeiPlugin.setGoalRegistry().",
                    "[Millenaire-JEI] L'instance GoalRegistry est nulle ! Impossible de charger les recettes de fabrication Millénaire pour les Handlers cibles : {}. Veuillez vérifier le moment de l'affectation de MillenaireJeiPlugin.setGoalRegistry().",
                    targetHandlerIds
            );
            return recipes;
        }
        // 使用容错后(真正激活)的 activeRegistry 提取目标 {@link GatheringGoal} 列表
        List<GatheringGoal> gatheringGoals = activeRegistry.getGatheringGoals();

        int totalGoals = gatheringGoals != null ? gatheringGoals.size() : 0;
        // [醒目新注释] 日志中增加 targetHandlerIds 打印，清晰展现当前调用的 Handlers 集合
        MMLog.info(
                "[Millenaire-JEI] 开始解析千年工艺配方，目标 Handlers: {}, 获取到的 GatheringGoal 总数: {}",
                "[Millenaire-JEI] Starting parsing Millénaire crafting recipes. Target Handlers: {}, Total GatheringGoals fetched: {}",
                "[Millenaire-JEI] Début de l'analyse des recettes de fabrication Millénaire, Handlers cibles : {}, nombre total de GatheringGoal obtenus : {}",
                targetHandlerIds, totalGoals
        );

        if (gatheringGoals == null || gatheringGoals.isEmpty()) {
            // [醒目新注释] 警告日志追加 targetHandlerIds，指示数据源为空时对应的分类类别
            MMLog.warn(
                    "[Millenaire-JEI] GatheringGoal 列表为空，未找到任何采集与加工 Goal 数据！目标 Handlers: {}",
                    "[Millenaire-JEI] GatheringGoal list is empty, no gathering or processing Goal data found! Target Handlers: {}",
                    "[Millenaire-JEI] La liste GatheringGoal est vide, aucune donnée Goal de collecte ou de fabrication trouvée ! Handlers cibles : {}",
                    targetHandlerIds
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
                // 只要属于目标 Handler 类均进行解析
                if (targetHandlerIds.contains(handlerId)) {
                    matchedCount++;

                    // 检索当前配方类型绑定的所有村民类型
                    List<VillagerType> matchedVillagers = goalToVillagersMap.getOrDefault(type.id(), List.of());

                    /* =========================================================================================
                     * [新注释] 【分流路由与类型修正】
                     * 调用重构后的静态工厂 {@link MillCraftingRecipe#parse(GatheringType, List)}，
                     * 直接获取解析完成的 {@link IMillRecipe} 子类实体（如 {@link StandardCraftingRecipe}）。
                     * ========================================================================================= */
                    IMillRecipe recipe = MillCraftingRecipe.parse(type, matchedVillagers);
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

        // [醒目新注释] 统计日志中补充打印 targetHandlerIds 集合内容
        MMLog.info(
                "[Millenaire-JEI] 配方提取完成。目标 Handlers: {}, 匹配: {} 个，成功解析配方: {} 个。",
                "[Millenaire-JEI] Recipe extraction complete. Target Handlers: {}, matched: {}, successfully parsed recipes: {}.",
                "[Millenaire-JEI] Extraction des recettes terminée. Handlers cibles : {}, correspondants : {}, recettes analysées avec succès : {}.",
                targetHandlerIds, matchedCount, parsedCount
        );

        if (!recipes.isEmpty()) {
            // [醒目新注释] 成功注册日志补充打印 targetHandlerIds
            MMLog.info(
                    "[Millenaire-JEI] 已向 JEI 成功注册 {} 个千年工艺/加工配方！目标 Handlers: {}",
                    "[Millenaire-JEI] Successfully registered {} Millénaire crafting/processing recipes to JEI! Target Handlers: {}",
                    "[Millenaire-JEI] Enregistrement réussi de {} recettes de fabrication/cuisine Millénaire dans JEI ! Handlers cibles : {}",
                    recipes.size(), targetHandlerIds
            );
        } else {
            // [醒目新注释] 提示隐藏页签时精准打印出空配方的 targetHandlerIds 集合
            MMLog.warn(
                    "[Millenaire-JEI] 目标 Handlers {} 最终生成的配方列表为空，JEI 将自动隐藏该 Category 页签！",
                    "[Millenaire-JEI] The final generated recipe list for target Handlers {} is empty. JEI will automatically hide this Category tab!",
                    "[Millenaire-JEI] La liste finale des recettes générées pour les Handlers cibles {} est vide, JEI masquera automatiquement cet onglet de catégorie !",
                    targetHandlerIds
            );
        }
        return recipes;
    }
    // endregion 3. 核心配方提取与匹配通用逻辑

    // region 4. Villager 与 Goal 绑定关系构建 API
    /**
     * 构建的目标 Goal / GatheringType ID 到对应村民类型列表的建立关系字典。
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
    // endregion 4. Villager 与 Goal 绑定关系构建 API
}