package com.momos.millenairejeim.jei.crafting.type;

import com.momos.millenairejeim.config.CustomGoalConfigEntry;
import com.momos.millenairejeim.config.GoalItemConfigSpec;
import com.momos.millenairejeim.config.MillJeiConfigManager;
import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.crafting.type.base.MillCraftingBaseRecipe;
import com.momos.millenairejeim.util.MMLog;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 纯外部配置驱动的千年村庄 Goal JEI 配方实体。
 * <p>数据 100% 来源于 {@code config/millenairejeim/custom_goals/*.json}，
 * 用于呈现官方或整合包难以全自动静态推演剩余目标（农作物/水稻/可可/树苗/花卉种植、建筑取货、工具获取、捡拾、点炉、修路等），
 * 同时自动绑定原版 {@link VillagerType} 与 {@link BuildingPlanSet}，无缝复用 {@code MillenaireJEIe} 加号详情跳转。</p>
 */
public class GoalConfigRecipe extends MillCraftingBaseRecipe {

    private final String sourceConfigFile;
    private final String titleKey;
    private final String titleFallback;
    private final List<ResourceLocation> boundGoalIds;
    private final List<BuildingPlanSet> explicitBuildings;
    private final List<String> buildingTags;
    private final boolean autoBindBuildingsFromVillagers;

    public GoalConfigRecipe(
            ResourceLocation id,
            String goalKey,
            String handlerId,
            List<IngredientWithCount> inputs,
            List<ItemStack> outputs,
            List<ItemStack> catalysts,
            List<VillagerType> villagerTypes,
            String sourceConfigFile,
            String titleKey,
            String titleFallback,
            List<ResourceLocation> boundGoalIds,
            List<BuildingPlanSet> explicitBuildings,
            List<String> buildingTags,
            boolean autoBindBuildingsFromVillagers
    ) {
        super(id, goalKey, handlerId, inputs, outputs, catalysts, villagerTypes);
        this.sourceConfigFile = sourceConfigFile != null ? sourceConfigFile : "";
        this.titleKey = titleKey != null ? titleKey : "";
        this.titleFallback = titleFallback != null ? titleFallback : "";
        this.boundGoalIds = boundGoalIds != null ? List.copyOf(boundGoalIds) : List.of();
        this.explicitBuildings = explicitBuildings != null ? List.copyOf(explicitBuildings) : List.of();
        this.buildingTags = buildingTags != null ? List.copyOf(buildingTags) : List.of();
        this.autoBindBuildingsFromVillagers = autoBindBuildingsFromVillagers;
    }

    public String getSourceConfigFile() {
        return sourceConfigFile;
    }

    public List<ResourceLocation> getBoundGoalIds() {
        return boundGoalIds;
    }

    public List<String> getBuildingTags() {
        return buildingTags;
    }

    @Override
    @Nullable
    public String getBuildingTag() {
        return buildingTags.isEmpty() ? null : String.join(", ", buildingTags);
    }

    @Override
    public Component getDisplayGoalName() {
        if (!titleKey.isBlank()) {
            String fallback = !titleFallback.isBlank() ? titleFallback : titleKey;
            return Component.translatableWithFallback(titleKey, fallback);
        }
        if (!titleFallback.isBlank()) {
            return Component.literal(titleFallback);
        }
        return MillenaireLocalizeHelper.getGoalName(getGoalKey());
    }

    @Override
    public List<BuildingPlanSet> getAssociatedBuildingIds() {
        Set<BuildingPlanSet> merged = new LinkedHashSet<>();
        if (explicitBuildings != null) {
            merged.addAll(explicitBuildings);
        }
        List<BuildingPlanSet> villagerBuildings = autoBindBuildingsFromVillagers
                ? super.getAssociatedBuildingIds()
                : Collections.emptyList();
        merged.addAll(villagerBuildings);

        List<BuildingPlanSet> current = new ArrayList<>(merged);
        for (String tag : buildingTags) {
            current = GoalSpotRecipe.resolveBuildingsWithTag(current, getVillagerTypes(), tag);
        }
        return current.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted(java.util.Comparator.comparing(b -> b.id() != null ? b.id().toString() : String.valueOf(b.buildingId())))
                .toList();
    }

    @Nullable
    public static GoalConfigRecipe fromConfig(
            CustomGoalConfigEntry entry,
            Map<ResourceLocation, List<VillagerType>> goalToVillagersMap
    ) {
        if (entry == null || !entry.isEnabled()) {
            return null;
        }
        String sourceFile = entry.sourceFile != null ? entry.sourceFile : "custom_goals";

        List<ResourceLocation> boundGoals = parseGoalIds(entry.goalIds);
        String goalKey = (entry.goalKey != null && !entry.goalKey.isBlank())
                ? entry.goalKey.trim()
                : (!boundGoals.isEmpty() ? boundGoals.get(0).getPath() : sanitizeFileStem(sourceFile));

        ResourceLocation recipeId = resolveRecipeId(entry.id, goalKey, sourceFile);
        String handlerId = (entry.handlerId != null && !entry.handlerId.isBlank())
                ? entry.handlerId.trim().toLowerCase(Locale.ROOT)
                : "custom_goal";

        List<IngredientWithCount> inputs = new ArrayList<>();
        if (entry.inputs != null) {
            for (GoalItemConfigSpec spec : entry.inputs) {
                if (spec == null) continue;
                IngredientWithCount ing = spec.toIngredientWithCount(sourceFile);
                if (ing != null) {
                    inputs.add(ing);
                }
            }
        }

        List<ItemStack> catalysts = new ArrayList<>();
        if (entry.catalysts != null) {
            for (GoalItemConfigSpec spec : entry.catalysts) {
                if (spec == null) continue;
                ItemStack stack = spec.toItemStack(sourceFile);
                if (!stack.isEmpty()) {
                    catalysts.add(stack);
                }
            }
        }

        List<ItemStack> outputs = new ArrayList<>();
        if (entry.outputs != null) {
            for (GoalItemConfigSpec spec : entry.outputs) {
                if (spec == null) continue;
                ItemStack stack = spec.toItemStack(sourceFile);
                if (!stack.isEmpty()) {
                    outputs.add(stack);
                }
            }
        }

        if (inputs.isEmpty() && catalysts.isEmpty() && outputs.isEmpty()) {
            MMLog.warn(
                    "[Millenaire-JEI] 配置 Goal '{}' ({}) 的 inputs/catalysts/outputs 均为空，已跳过展示。",
                    "[Millenaire-JEI] Configured Goal '{}' ({}) has empty inputs, catalysts, and outputs; skipped.",
                    "[Millenaire-JEI] Le Goal configuré '{}' ({}) n'a ni entrées, ni catalyseurs, ni sorties ; ignoré.",
                    recipeId, sourceFile
            );
            return null;
        }

        Set<VillagerType> matchedVillagers = new LinkedHashSet<>();
        if (entry.villagerIds != null) {
            for (String rawVid : entry.villagerIds) {
                VillagerType vt = resolveVillagerById(rawVid);
                if (vt != null) {
                    matchedVillagers.add(vt);
                }
            }
        }
        if (entry.shouldAutoBindGoalVillagers() && goalToVillagersMap != null) {
            for (ResourceLocation gid : boundGoals) {
                List<VillagerType> found = goalToVillagersMap.get(gid);
                if (found != null) {
                    matchedVillagers.addAll(found);
                }
            }
        }

        List<BuildingPlanSet> explicitBuildings = new ArrayList<>();
        if (entry.buildingIds != null) {
            for (String rawBid : entry.buildingIds) {
                if (rawBid == null || rawBid.isBlank()) continue;
                ResourceLocation bid = ResourceLocation.tryParse(rawBid.trim());
                if (bid != null) {
                    BuildingPlanSet bps = MillenaireAPIHelper.getBuildingPlanSet(bid);
                    if (bps != null && !explicitBuildings.contains(bps)) {
                        explicitBuildings.add(bps);
                    }
                }
            }
        }

        List<String> cleanTags = new ArrayList<>();
        if (entry.buildingTags != null) {
            for (String tag : entry.buildingTags) {
                if (tag != null && !tag.isBlank()) {
                    cleanTags.add(tag.trim());
                }
            }
        }

        GoalConfigRecipe recipe = new GoalConfigRecipe(
                recipeId,
                goalKey,
                handlerId,
                inputs,
                outputs,
                catalysts,
                new ArrayList<>(matchedVillagers),
                sourceFile,
                entry.titleKey,
                entry.title,
                boundGoals,
                explicitBuildings,
                cleanTags,
                entry.shouldAutoBindBuildingsFromVillagers()
        );

        if (entry.outputs != null) {
            for (GoalItemConfigSpec spec : entry.outputs) {
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

        if ((entry.extraInfoKey != null && !entry.extraInfoKey.isBlank())
                || (entry.extraInfo != null && !entry.extraInfo.isBlank())) {
            Component extraLine = MillJeiConfigManager.resolveComponent(entry.extraInfoKey, entry.extraInfo, entry.extraInfoArgs);
            List<Component> extraTooltips = MillJeiConfigManager.resolveComponentList(entry.extraTooltipKeys, entry.extraTooltips);
            recipe.setExtraInfo(extraLine, extraTooltips.isEmpty() ? null : extraTooltips);
        }

        if (entry.villagerSummary != null && !entry.villagerSummary.isBlank()) {
            recipe.setCustomVillagerSummary(Component.literal(entry.villagerSummary));
        }
        if (entry.buildingSummary != null && !entry.buildingSummary.isBlank()) {
            recipe.setCustomBuildingSummary(Component.literal(entry.buildingSummary));
        }
        return recipe;
    }

    private static List<ResourceLocation> parseGoalIds(@Nullable List<String> rawGoalIds) {
        if (rawGoalIds == null || rawGoalIds.isEmpty()) {
            return List.of();
        }
        List<ResourceLocation> parsed = new ArrayList<>();
        for (String raw : rawGoalIds) {
            if (raw == null || raw.isBlank()) continue;
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if (!normalized.contains(":")) {
                normalized = "millenaire:" + normalized;
            }
            ResourceLocation rl = ResourceLocation.tryParse(normalized);
            if (rl != null && !parsed.contains(rl)) {
                parsed.add(rl);
            }
        }
        return parsed;
    }

    private static ResourceLocation resolveRecipeId(@Nullable String rawId, String goalKey, String sourceFile) {
        if (rawId != null && !rawId.isBlank()) {
            ResourceLocation parsed = ResourceLocation.tryParse(rawId.trim().toLowerCase(Locale.ROOT));
            if (parsed != null) {
                return parsed;
            }
        }
        String path = "custom_" + sanitizeFileStem(goalKey != null && !goalKey.isBlank() ? goalKey : sourceFile);
        return ResourceLocation.fromNamespaceAndPath("millenaire", path);
    }

    private static String sanitizeFileStem(String raw) {
        String stem = raw.replace(".json", "").toLowerCase(Locale.ROOT);
        stem = stem.replaceAll("[^a-z0-9_./-]", "_");
        return stem.isEmpty() ? "unknown" : stem;
    }

    @Nullable
    private static VillagerType resolveVillagerById(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String trimmed = rawId.trim().toLowerCase(Locale.ROOT);
        ResourceLocation rl = ResourceLocation.tryParse(trimmed.contains(":") ? trimmed : ("millenaire:" + trimmed));
        if (rl == null) {
            return null;
        }
        VillagerType direct = MillenaireAPIHelper.getVillager(rl);
        if (direct != null) {
            return direct;
        }
        Collection<VillagerType> all = MillenaireAPIHelper.getAllVillagers();
        if (all != null) {
            String suffix = "/" + rl.getPath();
            for (VillagerType vt : all) {
                if (vt != null && vt.id() != null
                        && (vt.id().getPath().equals(rl.getPath()) || vt.id().getPath().endsWith(suffix))) {
                    return vt;
                }
            }
        }
        return null;
    }
}
