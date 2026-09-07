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
     * 获取商店[就是:文明-建筑(拥有交易能力)]的名称本地化组件 {@link Component}。
     * @param cultureId 文化 {@link ResourceLocation}
     * @param shopId 商店注册标识
     * @return 格式化后的本地化组件，若为空则使用 {@link MillenaireJeiKeys#KEY_MISSING_SHOP}
     */
    public static Component getShopText(ResourceLocation cultureId, String shopId) {
        if (cultureId == null || shopId == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_SHOP, MillenaireJeiKeys.FALLBACK_MISSING_SHOP);
        }
        return MillenaireLocalizeHelper.getCultureName(cultureId).append(" - ").append(MillenaireLocalizeHelper.getBuildingName(cultureId,shopId));
    }

    public static Component getCraftingVillagerText(VillagerType villagerType){
        if(villagerType == null){

        }
        return MillenaireLocalizeHelper.getCultureName(villagerType.culture()).append(" - ").append(MillenaireLocalizeHelper.getVillagerName(villagerType));
    }

    public static Component getCraftingBuildingText(BuildingPlanSet buildingPlanSet){
        if(buildingPlanSet == null){

        }
        return MillenaireLocalizeHelper.getCultureName(buildingPlanSet.culture()).append(" - ").append(MillenaireLocalizeHelper.getBuildingName(buildingPlanSet));
    }
}

