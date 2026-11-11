package com.momos.millenairejeim.config;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code config/millenairejeim/spot_gathering/*.json} 单条定点采集/处理配置模型。
 * <p>用于外部注入 {@link org.millenaire.goal.impl.SpotGatheringGoal} 的输入、催化剂、产出与附加说明，
 * 取代原有的 Java 硬编码 {@code switch (goalKey)}。</p>
 */
public class SpotGatheringConfigEntry {
    public Boolean enabled = Boolean.TRUE;
    public String goalId = "";
    public List<GoalItemConfigSpec> inputs = new ArrayList<>();
    public List<GoalItemConfigSpec> catalysts = new ArrayList<>();
    public List<GoalItemConfigSpec> outputs = new ArrayList<>();
    public Boolean useReflectedStockItemFallback = Boolean.TRUE;
    public String buildingTagOverride = "";
    public Integer townhallLimitOverride = null;
    public String extraInfoKey = "";
    public List<String> extraInfoArgs = new ArrayList<>();
    public String extraInfoText = "";
    public List<String> extraTooltipKeys = new ArrayList<>();
    public List<String> extraTooltips = new ArrayList<>();
    public List<String> outputTooltipKeys = new ArrayList<>();
    public List<String> outputTooltips = new ArrayList<>();

    public transient String sourceFile = "";

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    public boolean shouldUseReflectedStockItemFallback() {
        return useReflectedStockItemFallback == null || useReflectedStockItemFallback;
    }
}
