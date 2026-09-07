package com.momos.millenairejeim.helper;

import com.momos.millenairejeim.jei.MillenaireJeiKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

/**
 * 千年村庄 (Millenaire) 原版实体本地化 Key 解析助手类。
 * <p>负责接管文化、商店、村民、建筑及 Goal 配方的本地化 Key 格式化模板，
 * 并提供安全的 {@link Component} 转换与 fallback 解析。</p>
 */
public final class MillenaireLocalizeHelper {
    private MillenaireLocalizeHelper() {}
    // Millenaire Key Template
    // Todo:应该有个隐式转换的类，这样就可以直接传参了
    /** [未启用] 暂时没有文化本地化Key */
    public static final String TEMPLATE_CULTURE = "culture.%s.%s";
    /** 村民类型 Key 格式：{@code role.millenaire.<culture>_<villager_id>} */
    public static final String TEMPLATE_VILLAGER = "role.millenaire.%s_%s";
    /** 建筑 Key 格式：{@code building.millenaire.<culture>.<building_id>} */
    public static final String TEMPLATE_BUILDING = "building.millenaire.%s.%s";
    /** Goal 配方 Key 格式：{@code goal.millenaire.<goal_id>} */
    public static final String TEMPLATE_GOAL = "goal.millenaire.%s";

    /* ========================================================================= */
    /* 2. 本地化 Component 快速获取方法                                           */
    /* ========================================================================= */
    /**
     * 获取文化名称本地化组件 {@link Component}。
     * @param cultureId 文化 {@link ResourceLocation} 标识
     * @return 格式化后的本地化组件，若为空则使用 {@link MillenaireJeiKeys#KEY_MISSING_CULTURE}
     */
    public static MutableComponent getCultureName(ResourceLocation cultureId) {
        if (cultureId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_CULTURE, MillenaireJeiKeys.FALLBACK_MISSING_CULTURE);
        }
        // Key未实现
        if(false) {
            String key = String.format(TEMPLATE_CULTURE, cultureId.getNamespace(), cultureId.getPath());
            return Component.translatableWithFallback(key, cultureId.getPath());
        }
        // 直接使用文化的displayName
        return Component.literal(MillenaireAPIHelper.getCulture(cultureId).displayName());
    }

    /**
     * 获取村民角色本地化组件 {@link Component}。
     * @param culture 文化标识字符串
     * @param villagerId 村民类型 ID
     * @return 格式化后的本地化组件，若为空则使用 {@link MillenaireJeiKeys#KEY_MISSING_VILLAGER}
     */
    public static Component getVillagerName(String culture, String villagerId) {
        if (culture == null || villagerId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_VILLAGER, MillenaireJeiKeys.FALLBACK_MISSING_VILLAGER);
        }
        String key = String.format(TEMPLATE_VILLAGER, culture, villagerId);
        return Component.translatableWithFallback(key, villagerId);
    }
    /**
     * 根据 {@link VillagerType} 获取村民本地化组件 {@link Component}。
     * <p>优先从 {@code villagerType.id().getPath()} 中按 '/' 字符拆分，
     * 将 '/' 前的文化标识与 '/' 后的村民 Key 分别传入底层重载方法；
     * 若不包含 '/'，则退回使用 {@code villagerType.culture().getPath()} 与完整路径。</p>
     * @param villagerType 村民类型实体 {@link VillagerType}
     * @return 格式化后的本地化文本组件 {@link Component}
     */
    public static Component getVillagerName(VillagerType villagerType) {
        if (villagerType == null || villagerType.id() == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_VILLAGER, MillenaireJeiKeys.FALLBACK_MISSING_VILLAGER);
        }
        String path = villagerType.id().getPath(); // 获取如 "japanese/miner" 的路径
        int slashIndex = path.indexOf('/');
        // 醒目新增：按 '/' 拆分 path 的前后两部分，依次作为 cultureKey 与 villagerKey 传入
        if (slashIndex == -1) {
            // [新注释] 若原 ID 路径中不包含 '/'，退回使用 culture().getPath() 与原始 path 传入
            String cultureKey = villagerType.culture() != null ? villagerType.culture().getPath() : "";
            return getVillagerName(cultureKey, path);
        }
        String cultureKey = path.substring(0, slashIndex);     // '/' 前面部分（例如 "japanese"）
        String villagerKey = path.substring(slashIndex + 1);  // '/' 后面部分（例如 "miner"）
        return getVillagerName(cultureKey, villagerKey);
    }


    /**
     * 获取建筑模板本地化组件 {@link Component}。
     * @param culture 文化标识字符串
     * @param buildingId 建筑模板 ID
     * @return 格式化后的本地化组件，若为空则使用 {@link MillenaireJeiKeys#KEY_MISSING_BUILDING}
     */
    public static Component getBuildingName(String culture, String buildingId) {
        if (culture == null || buildingId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_BUILDING, MillenaireJeiKeys.FALLBACK_MISSING_BUILDING);
        }
        String key = String.format(TEMPLATE_BUILDING, culture, buildingId);
        return Component.translatableWithFallback(key, buildingId);
    }
    public static Component getBuildingName(ResourceLocation cultureId, String buildingId){ return getBuildingName(cultureId.getPath(),buildingId); }
    public static Component getBuildingName(BuildingPlanSet buildingPlanSet) { return getBuildingName(buildingPlanSet.culture().getPath(),buildingPlanSet.buildingId()); }

    /**
     * 获取 Goal 配方目标本地化组件 {@link Component}。
     * @param goalId 目标注册 ID
     * @return 格式化后的本地化组件，若为空则使用 {@link MillenaireJeiKeys#KEY_MISSING_GOAL}
     */
    public static Component getGoalName(String goalId) {
        if (goalId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_GOAL, MillenaireJeiKeys.FALLBACK_MISSING_GOAL);
        }
        String key = String.format(TEMPLATE_GOAL, goalId);
        return Component.translatableWithFallback(key, goalId);
    }
}