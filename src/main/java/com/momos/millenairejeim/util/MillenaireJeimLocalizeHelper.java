package com.momos.millenairejeim.util;

import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.MillenaireJeiKeys;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

import java.util.Collection;

public class MillenaireJeimLocalizeHelper {

    /**
     * 向 JEI 悬浮窗构建器 {@link ITooltipBuilder} 追加统一格式化的列表项 Component。
     * @param tooltip   JEI 悬浮窗构建器 {@link ITooltipBuilder}
     * @param entryName 列表项内容组件 {@link Component}
     */
    public static void addTooltipEntry(ITooltipBuilder tooltip, Component entryName) {
        if (tooltip != null && entryName != null) {
            tooltip.add(Component.translatableWithFallback(
                    MillenaireJeiKeys.KEY_TOOLTIP_ITEM_ENTRY,
                    MillenaireJeiKeys.FALLBACK_TOOLTIP_ITEM_ENTRY,
                    entryName
            ));
        }
    }

    /**
     * [新注释] 检查指定建筑 Key 的译名在同组 Key 集合中是否存在重名。
     * @param cultureId   文化 {@link ResourceLocation}
     * @param targetKey   待检测的建筑注册路径或 shopId
     * @param siblingKeys 同组所有建筑/商店 Key 集合
     * @return 是否存在译名重名
     */
    public static boolean isNameDuplicate(ResourceLocation cultureId, String targetKey, Collection<String> siblingKeys) {
        if (siblingKeys == null || siblingKeys.size() <= 1 || cultureId == null || targetKey == null) {
            return false;
        }
        String targetName = MillenaireLocalizeHelper.getBuildingName(cultureId, targetKey).getString();
        int matchCount = 0;
        for (String key : siblingKeys) {
            if (targetName.equals(MillenaireLocalizeHelper.getBuildingName(cultureId, key).getString())) {
                matchCount++;
                if (matchCount > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * [新注释] 获取单个建筑/商店的基础文本展示组件 {@link Component}。
     * 仅在 isDuplicate 为 true 时追加原始 Key 的括号后缀。
     * @param cultureId    文化 {@link ResourceLocation}
     * @param buildingPath 建筑注册路径或 shopId
     * @param isDuplicate  是否存在重名
     * @return 本地化组件 {@link Component}
     */
    public static Component getBuildingText(ResourceLocation cultureId, String buildingPath, boolean isDuplicate) {
        if (cultureId == null || buildingPath == null) {
            return Component.translatableWithFallback(MillenaireJeiKeys.KEY_MISSING_SHOP, MillenaireJeiKeys.FALLBACK_MISSING_SHOP);
        }
        MutableComponent nameComponent = MillenaireLocalizeHelper.getCultureName(cultureId)
                .append(" - ")
                .append(MillenaireLocalizeHelper.getBuildingName(cultureId, buildingPath));

        if (isDuplicate) {
            nameComponent.append(" (").append(buildingPath).append(")");
        }
        return nameComponent;
    }

    /**
     * 获取单个建筑/商店的基础文本展示组件 {@link Component},自动对传入的 siblingKeys 集合进行译名重名检测。
     * @param cultureId    文化 {@link ResourceLocation}
     * @param buildingPath 建筑注册路径或 shopId
     * @param siblingKeys  同组建筑 Key 集合，用于重名检测
     * @return 本地化组件 {@link Component}
     */
    public static Component getBuildingText(ResourceLocation cultureId, String buildingPath, Collection<String> siblingKeys) {
        boolean isDuplicate = isNameDuplicate(cultureId, buildingPath, siblingKeys);
        return getBuildingText(cultureId, buildingPath, isDuplicate);
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