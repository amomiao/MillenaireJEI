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

    /** [未启用] 暂时没有文化本地化Key */
    public static final String TEMPLATE_CULTURE = "culture.%s.%s";
    /** 村民类型 Key 格式：{@code role.millenaire.<culture>_<villager_id>} */
    public static final String TEMPLATE_VILLAGER = "role.millenaire.%s_%s";
    /** 建筑 Key 格式：{@code building.millenaire.<culture>.<building_id>} */
    public static final String TEMPLATE_BUILDING = "building.millenaire.%s.%s";
    /** Goal 配方 Key 格式：{@code goal.millenaire.<goal_id>} */
    public static final String TEMPLATE_GOAL = "goal.millenaire.%s";

    public static MutableComponent getCultureName(ResourceLocation cultureId) {
        if (cultureId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_CULTURE, MillenaireJeiKeys.FALLBACK_MISSING_CULTURE);
        }
        return Component.literal(MillenaireAPIHelper.getCulture(cultureId).displayName());
    }

    public static Component getVillagerName(String culture, String villagerId) {
        if (culture == null || villagerId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_VILLAGER, MillenaireJeiKeys.FALLBACK_MISSING_VILLAGER);
        }
        String key = String.format(TEMPLATE_VILLAGER, culture, villagerId);
        return Component.translatableWithFallback(key, villagerId);
    }

    public static Component getVillagerName(VillagerType villagerType) {
        if (villagerType == null || villagerType.id() == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_VILLAGER, MillenaireJeiKeys.FALLBACK_MISSING_VILLAGER);
        }
        String path = villagerType.id().getPath();
        int slashIndex = path.indexOf('/');
        if (slashIndex == -1) {
            String cultureKey = villagerType.culture() != null ? villagerType.culture().getPath() : "";
            return getVillagerName(cultureKey, path);
        }
        String cultureKey = path.substring(0, slashIndex);
        String villagerKey = path.substring(slashIndex + 1);
        return getVillagerName(cultureKey, villagerKey);
    }

    public static Component getBuildingName(String culture, String buildingId) {
        if (culture == null || buildingId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_BUILDING, MillenaireJeiKeys.FALLBACK_MISSING_BUILDING);
        }
        String key = String.format(TEMPLATE_BUILDING, culture, buildingId);
        return Component.translatableWithFallback(key, buildingId);
    }

    public static Component getBuildingName(ResourceLocation cultureId, String buildingId) {
        return getBuildingName(cultureId.getPath(), buildingId);
    }

    public static Component getBuildingName(BuildingPlanSet buildingPlanSet) {
        return getBuildingName(buildingPlanSet.culture().getPath(), buildingPlanSet.buildingId());
    }

    public static Component getGoalName(String goalId) {
        if (goalId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_GOAL, MillenaireJeiKeys.FALLBACK_MISSING_GOAL);
        }
        String key = String.format(TEMPLATE_GOAL, goalId);
        return Component.translatableWithFallback(key, goalId);
    }

    /**
     * 根据 Handler ID 获取工艺类型的本地化 Component。
     * <p>动态拼接 {@code jei.millenaire.crafting_type.<handler_id>} 语言包 Key。
     * 移除 switch 归并与 "村民手工" (default) 默认值，若未匹配到语言包则直接退化为原始 {@code handlerId} 名称。</p>
     * @param handlerId 配方处理句柄类型字符串
     * @return 本地化组件 {@link Component}
     */
    public static Component getCraftingTypeName(String handlerId) {
        if (handlerId == null || handlerId.trim().isEmpty()) {
            return Component.literal("Unknown");
        }
        String id = handlerId.toLowerCase();
        String craftingTypeKey = String.format(MillenaireJeiKeys.TEMPLATE_CRAFTING_TYPE, id);
        // [醒目新注释] 彻底移除 switch 映射与 default 常量，直接使用传入的 handlerId 原名作为 Fallback
        return Component.translatableWithFallback(craftingTypeKey, handlerId);
    }
}