package com.momos.millenairejeim.helper;

import com.momos.millenairejeim.util.VillagerBuildingMapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.millenaire.Millenaire;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.gathering.GatheringGoal;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.ItemHelper;
import org.millenaire.item.ModItems;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 千年村庄 (Millenaire) 模组核心 API 统一封装辅助类。
 * 集中管理对千年前后端核心 API 的调用，实现 JEI 插件与 Millenaire 核心逻辑的解耦与安全访问。
 */
public final class MillenaireAPIHelper {
    private static Map<ResourceLocation, List<BuildingPlanSet>> villagerBuildingMap;

    private MillenaireAPIHelper() {
    }

    // 获得文化信息
    public static Culture getCulture(ResourceLocation cultureId){ return ModCultures.getCulture(cultureId); }
    // 获得建筑信息
    public static BuildingPlanSet getBuildingPlanSet(ResourceLocation buildingId){ return ModCultures.getBuildingPlanSet(buildingId); }
    // 通过 ResourceLocation 获取对应的 Goal 逻辑实例
    public static VillagerGoal getGoal(ResourceLocation goalId) {return Millenaire.getGoalRegistry().get(goalId);}


    // region 物品与货币系统 API

    /**
     * 安全解析千年定义的物品字符串标识符，转换为标准的 Minecraft {@link Item}。
     *
     * @param itemId 物品标识符（如 ID 或标签名）
     * @return 解析后的 {@link Item} 实例
     * @see ItemHelper#resolve(String)
     */
    public static Item resolveItem(String itemId) {return ItemHelper.resolve(itemId);}

    /**
     * 获取千年金钱币 {@link ModItems#DENIER_OR} 堆叠。
     * @param count 数量
     * @return 金币 {@link ItemStack}
     * @see ModItems#DENIER_OR
     */
    public static ItemStack getDenierOrStack(int count) {return new ItemStack(ModItems.DENIER_OR.get(), count);}

    /**
     * 获取千年银钱币 {@link ModItems#DENIER_ARGENT} 堆叠。
     *
     * @param count 数量
     * @return 银币 {@link ItemStack}
     * @see ModItems#DENIER_ARGENT
     */
    public static ItemStack getDenierArgentStack(int count) {return new ItemStack(ModItems.DENIER_ARGENT.get(), count);}

    /**
     * 获取千年铜钱币 {@link ModItems#DENIER} 堆叠。
     *
     * @param count 数量
     * @return 铜币 {@link ItemStack}
     * @see ModItems#DENIER
     */
    public static ItemStack getDenierStack(int count) {return new ItemStack(ModItems.DENIER.get(), count);}

    // endregion 物品与货币系统 API

    /**
     * 获取所有已注册千年的文化 ResourceLocation 集合。
     * @return 文化 ResourceLocation 集合
     * @see ModCultures#getAllCultures()
     */
    public static Set<ResourceLocation> getAllCultureIds() {
        return ModCultures.getAllCultures().keySet();
    }

    // region 交易 API
    /**
     * 获取指定文化下的所有商店配置 {@link ShopProfile} 映射。
     * @param cultureId 文化 ResourceLocation 标识符
     * @return 商店 ID 到 {@link ShopProfile} 的映射表
     * @see ShopProfileLoader#getProfiles(ResourceLocation)
     */
    public static Map<String, ShopProfile> getShopProfiles(ResourceLocation cultureId) {return ShopProfileLoader.getProfiles(cultureId);}
    /**
     * 根据文化和商品 ID 检索特定的商业商品实体 {@link TradeGood}。
     * @param cultureId 文化 ResourceLocation
     * @param goodId    商品 ID
     * @return 对应的 {@link TradeGood}，未检索到则返回 null
     * @see TradeGoodsLoader#getGoodById(ResourceLocation, String)
     */
    public static TradeGood getTradeGood(ResourceLocation cultureId, String goodId) {return TradeGoodsLoader.getGoodById(cultureId, goodId);}
    /**
     * 从商品实体 {@link TradeGood} 中解析对应的 Minecraft {@link Item}。
     * @param good 商品实体
     * @return 解析后的 {@link Item} 实例，若 good 为 null 或无法解析则返回 null
     * @see TradeGood#resolveItem()
     */
    public static Item resolveTradeGoodItem(TradeGood good) {
        if (good == null) {
            return null;
        }
        return good.resolveItem();
    }
    /**
     * [新注释] 根据文化 ID 与商店标识查找所有对应的基础建筑标识 (Building Key) 集合。
     * 自动剥离变体 (_a) 与等级 (_2) 后缀，完成建筑集维度的去重。
     *
     * @param cultureId 文化 {@link ResourceLocation}
     * @param shopId    商店标识
     * @return 匹配到的基础建筑 Key 集合 {@link Set}（例如: ["castlepolish", "mansionpolish"]）
     */
    public static Set<String> getBuildingKeysByShopId(ResourceLocation cultureId, String shopId) {
        Set<String> matchedBuildingKeys = new HashSet<>();
        if (cultureId == null || shopId == null) {
            return matchedBuildingKeys;
        }
        Map<ResourceLocation, BuildingPlan> buildingMap = getAllBuildingPlans();
        if (buildingMap != null) {
            for (BuildingPlan plan : buildingMap.values()) {
                // [新注释] 匹配文化与商店 ID
                if (plan != null && shopId.equals(plan.shopId()) && cultureId.equals(plan.culture())) {
                    String baseKey = extractBaseBuildingKey(plan.id());
                    if (!baseKey.isEmpty()) {
                        matchedBuildingKeys.add(baseKey);
                    }
                }
            }
        }
        return matchedBuildingKeys;
    }
    /**
     * [新注释] 从 BuildingPlan 的 ResourceLocation 中提取基础建筑 Key。
     * 例如："millenaire:polish/castlepolish_a_2" -> "castlepolish"
     */
    public static String extractBaseBuildingKey(ResourceLocation planId) {
        if (planId == null) return "";
        String path = planId.getPath();

        // 1. 剥离文化路径前缀 (如 "polish/castlepolish_a_2" -> "castlepolish_a_2")
        if (path.contains("/")) {
            path = path.substring(path.lastIndexOf('/') + 1);
        }
        // 2. 剥离变体字母与等级数字后缀 (如 "castlepolish_a_2" -> "castlepolish")
        return path.replaceAll("_[a-zA-Z]_\\d+$", "");
    }
    // endregion 交易 API


    // region Goal API
    /**
     * 获取千年模组主目标注册表 {@link GoalRegistry} 实例。
     * @return 当前激活的 {@link GoalRegistry}，若未完成初始化则可能返回 null
     * @see Millenaire#getGoalRegistry()
     */
    public static GoalRegistry getGoalRegistry() {return Millenaire.getGoalRegistry();}

    /**
     * 从目标注册表中提取所有的采集/加工目标列表 {@link GatheringGoal}。
     * @param registry 千年目标注册表 {@link GoalRegistry}
     * @return 包含所有采集与加工 Goal 的列表，若 registry 为 null 则返回 null
     * @see GoalRegistry#getGatheringGoals()
     */
    public static List<GatheringGoal> getGatheringGoals(GoalRegistry registry) {
        if (registry == null) {
            return null;
        }
        return registry.getGatheringGoals();
    }

    /**
     * 从指定的 {@link GatheringGoal} 中获取对应的 {@link GatheringType} 实体。
     *
     * @param goal 目标实体
     * @return 关联的 {@link GatheringType}，若 goal 为 null 则返回 null
     * @see GatheringGoal#getGatheringType()
     */
    public static GatheringType getGatheringType(GatheringGoal goal) {
        if (goal == null) {
            return null;
        }
        return goal.getGatheringType();
    }
    // endregion Goal API

    // region Building&Villager API

    public static Map<ResourceLocation, VillagerType>  getAllVillagerType(){ return ModCultures.getAllVillagerTypes(); }
    /**获取全量注册的建筑计划集（按建筑类型分类，包含该建筑的所有升级阶段）。
     * @return 建筑 ID 到 {@link BuildingPlanSet} 的映射字典
     * @see ModCultures#getAllBuildingPlanSets()
     */
    public static Map<ResourceLocation, BuildingPlanSet> getAllBuildingPlanSets() {return ModCultures.getAllBuildingPlanSets();}
    /**获取全量注册的具体建筑蓝图（包含各个级别的独立蓝图）。
     * @return 蓝图 ID 到 {@link BuildingPlan} 的映射字典
     * @see ModCultures#getAllBuildingPlans()
     */
    public static Map<ResourceLocation, BuildingPlan> getAllBuildingPlans() {return ModCultures.getAllBuildingPlans();}

    /**
     * 根据村民类型 ID 查询其关联的所有建筑模板 {@link BuildingPlanSet} 列表。
     * @param villagerTypeId 村民类型的全路径 {@link ResourceLocation}
     * @return 此村民Type被注册于哪些建筑中 {@link BuildingPlanSet} 列表；若无匹配项则返回空列表
     */
    public static List<BuildingPlanSet> MappingVillagerBuilding(ResourceLocation villagerTypeId) {
        if (villagerTypeId == null) {
            return Collections.emptyList();
        }
        if (villagerBuildingMap == null) {
            // [新注释] 获取 ID 映射表后，通过 ModCultures#getBuildingPlanSet 将建筑 ID 解析为具体的 BuildingPlanSet 对象
            Map<ResourceLocation, List<ResourceLocation>> rawIdMap = VillagerBuildingMapper.buildVillagerToBuildingMap();

            villagerBuildingMap = rawIdMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .map(MillenaireAPIHelper::getBuildingPlanSet)
                                    .filter(Objects::nonNull)
                                    .toList()
                    ));
        }
        return villagerBuildingMap.getOrDefault(villagerTypeId, Collections.emptyList());
    }

    /**
     * 根据村民类型对象重载查询其关联的所有建筑模板 {@link BuildingPlanSet} 列表。
     * @param villagerType 村民类型配置对象 {@link VillagerType}
     * @return 该村民可居住或工作的建筑模板 {@link BuildingPlanSet} 列表
     */
    public static List<BuildingPlanSet> MappingVillagerBuilding(VillagerType villagerType) {
        if (villagerType == null) {
            return Collections.emptyList();
        }
        return MappingVillagerBuilding(villagerType.id());
    }
    // endregion Building&Villager API
}