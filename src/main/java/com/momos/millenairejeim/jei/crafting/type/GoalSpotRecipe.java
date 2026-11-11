package com.momos.millenairejeim.jei.crafting.type;

import com.momos.millenairejeim.config.GoalItemConfigSpec;
import com.momos.millenairejeim.config.MillJeiConfigManager;
import com.momos.millenairejeim.config.SpotGatheringConfigEntry;
import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.helper.SafeReflectionHelper;
import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.type.base.MillCraftingBaseRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.impl.SpotGatheringGoal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 对应千年 {@link SpotGatheringGoal}（定点采集/处理 Goal）的 JEI 配方实体。
 * <p>输入、催化剂、产出与额外说明完全由 {@code config/millenairejeim/spot_gathering/*.json} 外部配置注入；
 * 当自动化检测到未注入外部配置的新 {@link SpotGatheringGoal} 时，会输出双写告警日志并安全尝试按反射字段 {@code stockItem} 降级显示。</p>
 */
public class GoalSpotRecipe extends MillCraftingBaseRecipe {

    private final String buildingTag;
    private final int townhallLimit;

    public GoalSpotRecipe(
            ResourceLocation id,
            String goalKey,
            List<IngredientWithCount> inputs,
            List<ItemStack> outputs,
            List<ItemStack> catalysts,
            List<VillagerType> villagerTypes,
            String buildingTag,
            int townhallLimit
    ) {
        super(id, goalKey, "spot_gathering", inputs, outputs, catalysts, villagerTypes);
        this.buildingTag = buildingTag != null ? buildingTag : "";
        this.townhallLimit = townhallLimit;
        setTagConstraints(this.buildingTag, null);
    }

    public String getBuildingTag() {
        return buildingTag;
    }

    public int getTownhallLimit() {
        return townhallLimit;
    }

    @Override
    public List<BuildingPlanSet> getAssociatedBuildingIds() {
        return super.getAssociatedBuildingIds();
    }

    /** 按村民关联建筑与指定建筑标签解析并汇总建筑集合（同步服务 {@link GoalConfigRecipe}）。*/
    public static List<BuildingPlanSet> resolveBuildingsWithTag(
            List<BuildingPlanSet> baseBuildings,
            List<VillagerType> villagerTypes,
            @Nullable String buildingTag
    ) {
        Set<BuildingPlanSet> merged = new LinkedHashSet<>(baseBuildings != null ? baseBuildings : List.of());
        if (buildingTag != null && !buildingTag.isBlank()) {
            Set<ResourceLocation> villagerCultures = new LinkedHashSet<>();
            if (villagerTypes != null) {
                for (VillagerType vt : villagerTypes) {
                    if (vt != null && vt.culture() != null) {
                        villagerCultures.add(vt.culture());
                    }
                }
            }
            merged.addAll(MillenaireAPIHelper.getBuildingsByTag(buildingTag, null, villagerCultures));
        }
        return merged.stream()
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(b -> b.id() != null ? b.id().toString() : String.valueOf(b.buildingId())))
                .toList();
    }

    /**
     * 安全结合 {@link SpotGatheringGoal} 运行时反射字段与 {@code spot_gathering/} 外部配置创建 JEI 配方。
     */
    @Nullable
    public static GoalSpotRecipe create(ResourceLocation goalId, SpotGatheringGoal spotGoal, List<VillagerType> villagers) {
        if (goalId == null || spotGoal == null) {
            return null;
        }

        String buildingTag = SafeReflectionHelper.readStringField(spotGoal, "buildingTag");
        Item stockItem = SafeReflectionHelper.readSupplierField(spotGoal, "stockItem", Item.class);
        int townhallLimit = SafeReflectionHelper.readIntField(spotGoal, "townhallLimit", 0);

        SpotGatheringConfigEntry config = MillJeiConfigManager.findSpotConfig(goalId);
        List<IngredientWithCount> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        List<ItemStack> catalysts = new ArrayList<>();

        Component extraLine = null;
        List<Component> extraTooltip = List.of();
        List<Component> sharedOutputTooltips = List.of();

        if (config == null) {
            String stockKey = (stockItem != null && stockItem != Items.AIR)
                    ? BuiltInRegistries.ITEM.getKey(stockItem).toString()
                    : "none";
            MillJeiConfigManager.warnUninjectedSpotGoal(goalId, stockKey, buildingTag, townhallLimit);
            if (stockItem != null && stockItem != Items.AIR) {
                outputs.add(new ItemStack(stockItem));
            }
        } else {
            if (!config.isEnabled()) {
                return null;
            }
            String sourceFile = config.sourceFile != null ? config.sourceFile : "spot_gathering";
            if (config.inputs != null) {
                for (GoalItemConfigSpec spec : config.inputs) {
                    if (spec == null) continue;
                    IngredientWithCount ing = spec.toIngredientWithCount(sourceFile);
                    if (ing != null) {
                        inputs.add(ing);
                    }
                }
            }
            if (config.catalysts != null) {
                for (GoalItemConfigSpec spec : config.catalysts) {
                    if (spec == null) continue;
                    ItemStack stack = spec.toItemStack(sourceFile);
                    if (!stack.isEmpty()) {
                        catalysts.add(stack);
                    }
                }
            }
            if (config.outputs != null) {
                for (GoalItemConfigSpec spec : config.outputs) {
                    if (spec == null) continue;
                    ItemStack stack = spec.toItemStack(sourceFile);
                    if (!stack.isEmpty()) {
                        outputs.add(stack);
                    }
                }
            }
            if (outputs.isEmpty() && config.shouldUseReflectedStockItemFallback()
                    && stockItem != null && stockItem != Items.AIR) {
                outputs.add(new ItemStack(stockItem));
            }
            if (config.buildingTagOverride != null && !config.buildingTagOverride.isBlank()) {
                buildingTag = config.buildingTagOverride.trim();
            }
            if (config.townhallLimitOverride != null) {
                townhallLimit = config.townhallLimitOverride;
            }
            if ((config.extraInfoKey != null && !config.extraInfoKey.isBlank())
                    || (config.extraInfoText != null && !config.extraInfoText.isBlank())) {
                extraLine = MillJeiConfigManager.resolveComponent(config.extraInfoKey, config.extraInfoText, config.extraInfoArgs);
            }
            extraTooltip = MillJeiConfigManager.resolveComponentList(config.extraTooltipKeys, config.extraTooltips);
            sharedOutputTooltips = MillJeiConfigManager.resolveComponentList(config.outputTooltipKeys, config.outputTooltips);
        }

        if (inputs.isEmpty() && catalysts.isEmpty() && outputs.isEmpty()) {
            return null;
        }

        String goalKey = goalId.getPath();
        GoalSpotRecipe recipe = new GoalSpotRecipe(
                goalId, goalKey, inputs, outputs, catalysts, villagers, buildingTag, townhallLimit
        );

        if (extraLine != null) {
            recipe.setExtraInfo(extraLine, extraTooltip.isEmpty() ? null : extraTooltip);
        }
        if (config != null && config.outputs != null) {
            String sourceFile = config.sourceFile != null ? config.sourceFile : "spot_gathering";
            for (GoalItemConfigSpec spec : config.outputs) {
                if (spec == null) continue;
                ItemStack stack = spec.toItemStack(sourceFile);
                if (!stack.isEmpty()) {
                    List<Component> itemTips = spec.buildTooltips();
                    if (!itemTips.isEmpty()) {
                        recipe.addOutputTooltips(stack.getItem(), itemTips);
                    }
                }
            }
        }
        if (!sharedOutputTooltips.isEmpty()) {
            for (ItemStack out : outputs) {
                recipe.addOutputTooltips(out.getItem(), sharedOutputTooltips);
            }
        }
        if (townhallLimit > 0) {
            Component limitTip = Component.translatableWithFallback(
                    MillenaireJeimKeys.KEY_TOOLTIP_SPOT_TOWNHALL_LIMIT,
                    MillenaireJeimKeys.FALLBACK_TOOLTIP_SPOT_TOWNHALL_LIMIT,
                    townhallLimit
            );
            for (ItemStack out : outputs) {
                recipe.addOutputTooltip(out.getItem(), limitTip);
            }
        }
        return recipe;
    }
}
