package com.momos.millenairejeim.config;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code config/millenairejeim/custom_goals/*.json} 纯外部配置型 Goal 的 JEI 配方定义模型。
 * <p>支持绑定一个或多个原版/千年 Goal ID、Handler 类型、物品栏槽位、建筑标签以及自定义说明文本，
 * 并可自动联动匹配执行村民与建筑详情页。</p>
 */
public class CustomGoalConfigEntry {
    public Boolean enabled = Boolean.TRUE;
    public String id = "";
    public String goalKey = "";
    public List<String> goalIds = new ArrayList<>();
    public String handlerId = "custom_goal";
    public String titleKey = "";
    public String title = "";
    public List<GoalItemConfigSpec> inputs = new ArrayList<>();
    public List<GoalItemConfigSpec> catalysts = new ArrayList<>();
    public List<GoalItemConfigSpec> outputs = new ArrayList<>();
    public String extraInfoKey = "";
    public List<String> extraInfoArgs = new ArrayList<>();
    public String extraInfo = "";
    public List<String> extraTooltipKeys = new ArrayList<>();
    public List<String> extraTooltips = new ArrayList<>();
    public List<String> villagerIds = new ArrayList<>();
    public String villagerSummary = "";
    public List<String> buildingIds = new ArrayList<>();
    public List<String> buildingTags = new ArrayList<>();
    public String buildingSummary = "";
    public Boolean autoBindGoalVillagers = Boolean.TRUE;
    public Boolean autoBindBuildingsFromVillagers = Boolean.TRUE;
    public Integer sortOrder = 100;

    public transient String sourceFile = "";

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    public boolean shouldAutoBindGoalVillagers() {
        return autoBindGoalVillagers == null || autoBindGoalVillagers;
    }

    public boolean shouldAutoBindBuildingsFromVillagers() {
        return autoBindBuildingsFromVillagers == null || autoBindBuildingsFromVillagers;
    }

    public int getSortOrder() {
        return sortOrder != null ? sortOrder : 100;
    }
}
