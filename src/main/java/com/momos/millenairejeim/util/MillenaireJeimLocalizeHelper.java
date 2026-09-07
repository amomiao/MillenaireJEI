package com.momos.millenairejeim.util;

import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.MillenaireJeiKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

// 复合的高级API
public class MillenaireJeimLocalizeHelper {

    /**
     * 获取单个建筑/商店的基础文本展示组件 {@link Component}，并在末尾附加括号括起来的原始 Key。
     * 格式示例："拜占庭(罗马) - 军备工坊 (armory)"
     * @param cultureId    文化 {@link ResourceLocation}
     * @param buildingPath 建筑注册路径或 shopId
     * @return 带有原始 Key 的本地化组件 {@link Component}
     */
    public static Component getBuildingText(ResourceLocation cultureId, String buildingPath) {
        if (cultureId == null || buildingPath == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_SHOP, MillenaireJeiKeys.FALLBACK_MISSING_SHOP);
        }
        // [新注释] 拼接中文名称并在末尾使用括号附带原始 Key，方便区分同名建筑
        return MillenaireLocalizeHelper.getCultureName(cultureId)
                .append(" - ")
                .append(MillenaireLocalizeHelper.getBuildingName(cultureId, buildingPath))
                .append(" (")
                .append(buildingPath)
                .append(")");
    }

    public static Component getCraftingVillagerText(VillagerType villagerType){
        if(villagerType == null){
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_VILLAGER, MillenaireJeiKeys.FALLBACK_MISSING_VILLAGER);
        }
        return MillenaireLocalizeHelper.getCultureName(villagerType.culture()).append(" - ").append(MillenaireLocalizeHelper.getVillagerName(villagerType));
    }

    public static Component getCraftingBuildingText(BuildingPlanSet buildingPlanSet){
        if(buildingPlanSet == null){
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_BUILDING, MillenaireJeiKeys.FALLBACK_MISSING_BUILDING);
        }
        return MillenaireLocalizeHelper.getCultureName(buildingPlanSet.culture()).append(" - ").append(MillenaireLocalizeHelper.getBuildingName(buildingPlanSet));
    }
}