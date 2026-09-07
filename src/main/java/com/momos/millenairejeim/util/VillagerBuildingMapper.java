package com.momos.millenairejeim.util;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import java.util.*;

/**
 * 千年村庄村民与建筑关系反向映射工具类。
 * 用于建立 VillagerType -> List<BuildingPlanSet> 的一对多查找关系。
 */
public class VillagerBuildingMapper {
    /**
     * 构建 村民 ID -> 关联建筑 ID 列表 的反向映射字典。
     *
     * @return key 为村民全路径 ID，value 为可产生/居住该村民的建筑 ID 列表
     * @see ModCultures#getAllBuildingPlanSets()
     */
    public static Map<ResourceLocation, List<ResourceLocation>> buildVillagerToBuildingMap() {
        Map<ResourceLocation, List<ResourceLocation>> resultMap = new HashMap<>();
        // 遍历所有注册的建筑计划集 (BuildingPlanSet)
        for (Map.Entry<ResourceLocation, BuildingPlanSet> entry : MillenaireAPIHelper.getAllBuildingPlanSets().entrySet()) {
            ResourceLocation buildingId = entry.getKey();
            BuildingPlanSet planSet = entry.getValue();
            // 收集该建筑定义的所有男性和女性村民
            List<String> allVillagersInBuilding = new ArrayList<>();
            if (planSet.maleResidents() != null) allVillagersInBuilding.addAll(planSet.maleResidents());
            if (planSet.femaleResidents() != null) allVillagersInBuilding.addAll(planSet.femaleResidents());
            for (String rawVillagerName : allVillagersInBuilding) {
                // 将相对路径名称 (如 "glassblower") 转化为全路径 ID (如 "millenaire:norman/glassblower")
                ResourceLocation villagerId = resolveVillagerId(planSet.culture(), rawVillagerName);
                // 建立反向映射关系
                resultMap.computeIfAbsent(villagerId, k -> new ArrayList<>()).add(buildingId);
            }
        }
        return resultMap;
    }

    /**
     * 将文化与村民相对名称补全为标准的 ResourceLocation。
     * @param cultureId 文化 ID (如 "millenaire:norman")
     * @param villagerName 村民相对名 (如 "glassblower")
     * @return 村民完整 ID (如 "millenaire:norman/glassblower")
     */
    private static ResourceLocation resolveVillagerId(ResourceLocation cultureId, String villagerName) {
        if (villagerName.contains(":")) {
            return ResourceLocation.parse(villagerName);
        }
        return ResourceLocation.fromNamespaceAndPath(
                cultureId.getNamespace(),
                cultureId.getPath() + "/" + villagerName
        );
    }
}