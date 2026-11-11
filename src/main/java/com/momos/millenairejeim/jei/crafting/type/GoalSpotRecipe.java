package com.momos.millenairejeim.jei.crafting.type;

import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.helper.SafeReflectionHelper;
import com.momos.millenairejeim.jei.MillenaireJeimKeys;
import com.momos.millenairejeim.jei.crafting.type.base.MillCraftingBaseRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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
 * 对应千年 {@link SpotGatheringGoal}（Java 硬编码定点采集/处理 Goal）的 JEI 配方实体。
 * 覆盖蚕茧采集、蜗牛紫染料、泥砖收集、湿砖日光晒干、甘蔗种植与收割等。
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
    }

    public String getBuildingTag() {
        return buildingTag;
    }

    public int getTownhallLimit() {
        return townhallLimit;
    }

    @Override
    public List<BuildingPlanSet> getAssociatedBuildingIds() {
        List<BuildingPlanSet> buildings = new ArrayList<>(super.getAssociatedBuildingIds());
        if (buildingTag.isEmpty()) {
            return buildings;
        }
        Collection<BuildingPlanSet> allBuildings = MillenaireAPIHelper.getAllBuildings();
        if (allBuildings == null || allBuildings.isEmpty()) {
            return buildings;
        }

        Set<ResourceLocation> villagerCultures = new LinkedHashSet<>();
        for (VillagerType vt : getVillagerTypes()) {
            if (vt != null && vt.culture() != null) {
                villagerCultures.add(vt.culture());
            }
        }

        for (BuildingPlanSet bps : allBuildings) {
            if (bps == null || bps.tags() == null) continue;
            if (!villagerCultures.isEmpty() && (bps.culture() == null || !villagerCultures.contains(bps.culture()))) {
                continue;
            }
            boolean hasTag = bps.tags().stream().anyMatch(tag -> buildingTag.equalsIgnoreCase(tag));
            if (hasTag && !buildings.contains(bps)) {
                buildings.add(bps);
            }
        }
        return buildings;
    }

    /**
     * 安全从 {@link SpotGatheringGoal} 实例提取 JEI 配方，所有私有字段均通过 {@link SafeReflectionHelper} 读取。
     */
    @Nullable
    public static GoalSpotRecipe create(ResourceLocation goalId, SpotGatheringGoal spotGoal, List<VillagerType> villagers) {
        if (goalId == null || spotGoal == null) {
            return null;
        }

        String buildingTag = SafeReflectionHelper.readStringField(spotGoal, "buildingTag");
        Item stockItem = SafeReflectionHelper.readSupplierField(spotGoal, "stockItem", Item.class);
        int townhallLimit = SafeReflectionHelper.readIntField(spotGoal, "townhallLimit", 0);

        List<IngredientWithCount> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        List<ItemStack> catalysts = new ArrayList<>();

        Component extraLine = null;
        List<Component> extraTooltip = null;
        Component plantTip = null;

        String goalKey = goalId.getPath();
        switch (goalKey) {
            case "gather_silk" -> {
                addResolvedInput("millenaire:silk_worm", inputs);
                Item out = resolveWithFallback(stockItem, "millenaire:silk");
                if (out != Items.AIR) outputs.add(new ItemStack(out));
                extraLine = Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_INFO_RIPE_STAGE, MillenaireJeimKeys.FALLBACK_INFO_RIPE_STAGE, 3
                );
            }
            case "gather_snails" -> {
                addResolvedInput("millenaire:snail_soil", inputs);
                Item out = resolveWithFallback(stockItem, "minecraft:purple_dye");
                if (out != Items.AIR) outputs.add(new ItemStack(out));
                extraLine = Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_INFO_RIPE_STAGE, MillenaireJeimKeys.FALLBACK_INFO_RIPE_STAGE, 3
                );
            }
            case "gather_brick" -> {
                Item mudBrick = resolveWithFallback(stockItem, "millenaire:mud_brick");
                if (mudBrick != Items.AIR) {
                    catalysts.add(new ItemStack(mudBrick));
                    outputs.add(new ItemStack(mudBrick));
                }
            }
            case "dry_brick" -> {
                addResolvedInput("millenaire:wet_brick", inputs);
                Item mudBrick = resolveWithFallback(stockItem, "millenaire:mud_brick");
                if (mudBrick != Items.AIR) outputs.add(new ItemStack(mudBrick));
                extraLine = Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_INFO_SPOT_SUN_DRY, MillenaireJeimKeys.FALLBACK_INFO_SPOT_SUN_DRY
                );
                extraTooltip = List.of(Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_TOOLTIP_SPOT_SUN_DRY, MillenaireJeimKeys.FALLBACK_TOOLTIP_SPOT_SUN_DRY
                ));
            }
            case "harvest_sugar_cane" -> {
                addResolvedInput("minecraft:sugar_cane", inputs);
                Item cane = resolveWithFallback(stockItem, "minecraft:sugar_cane");
                if (cane != Items.AIR) outputs.add(new ItemStack(cane));
            }
            case "plant_sugar_cane" -> {
                addResolvedInput("minecraft:sugar_cane", inputs);
                outputs.add(new ItemStack(Items.SUGAR_CANE));
                extraLine = Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_INFO_SPOT_PLANT, MillenaireJeimKeys.FALLBACK_INFO_SPOT_PLANT
                );
                plantTip = Component.translatableWithFallback(
                        MillenaireJeimKeys.KEY_TOOLTIP_SPOT_PLANT, MillenaireJeimKeys.FALLBACK_TOOLTIP_SPOT_PLANT
                );
                extraTooltip = List.of(plantTip);
            }
            default -> {
                if (stockItem != null && stockItem != Items.AIR) {
                    outputs.add(new ItemStack(stockItem));
                }
            }
        }

        if (inputs.isEmpty() && catalysts.isEmpty() && outputs.isEmpty()) {
            return null;
        }

        GoalSpotRecipe recipe = new GoalSpotRecipe(
                goalId, goalKey, inputs, outputs, catalysts, villagers, buildingTag, townhallLimit
        );

        if (extraLine != null) {
            recipe.setExtraInfo(extraLine, extraTooltip);
        }
        if (plantTip != null) {
            recipe.addOutputTooltip(Items.SUGAR_CANE, plantTip);
        }
        if ("dry_brick".equals(goalKey) && extraTooltip != null) {
            for (ItemStack out : outputs) {
                recipe.addOutputTooltips(out.getItem(), extraTooltip);
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

    private static void addResolvedInput(String itemId, List<IngredientWithCount> inputs) {
        Item item = MillenaireAPIHelper.resolveItem(itemId);
        if (item != null && item != Items.AIR) {
            inputs.add(new IngredientWithCount(Ingredient.of(item), 1));
        }
    }

    private static Item resolveWithFallback(@Nullable Item preferred, String fallbackId) {
        if (preferred != null && preferred != Items.AIR) {
            return preferred;
        }
        Item resolved = MillenaireAPIHelper.resolveItem(fallbackId);
        return resolved != null ? resolved : Items.AIR;
    }
}
