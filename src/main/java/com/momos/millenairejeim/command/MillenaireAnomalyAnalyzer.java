package com.momos.millenairejeim.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.helper.SafeReflectionHelper;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.gathering.GatheringGoal;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.impl.SpotGatheringGoal;
import org.millenaire.item.ItemHelper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 千年工艺异常深度诊断辅助器。
 * <p>
 * 构建按文化划分及全局维度的物资生产源索引（涵盖 GatheringGoal、Java 硬编码 SpotGatheringGoal、
 * 隐式掉落规则与跨文化行商库存），用于识别无源输入（Unreachable Inputs）与产物语义不匹配（Semantic Mismatches）。
 * </p>
 */
final class MillenaireAnomalyAnalyzer {

    private static final Set<String> CHECKED_RECIPE_HANDLERS = Set.of("crafting", "smelting");

    private static final List<String[]> COMPETING_MATERIAL_PAIRS = List.of(
            new String[]{"silk", "wool"},
            new String[]{"silk", "cotton"},
            new String[]{"wool", "silk"},
            new String[]{"cotton", "silk"},
            new String[]{"gold", "iron"},
            new String[]{"iron", "gold"},
            new String[]{"diamond", "iron"},
            new String[]{"stone", "wood"}
    );

    private static final Map<ResourceLocation, String> FALLBACK_SPOT_GOAL_OUTPUTS = Map.of(
            ResourceLocation.fromNamespaceAndPath("millenaire", "gather_silk"), "millenaire:silk",
            ResourceLocation.fromNamespaceAndPath("millenaire", "gather_snails"), "minecraft:purple_dye",
            ResourceLocation.fromNamespaceAndPath("millenaire", "gather_brick"), "millenaire:mud_brick",
            ResourceLocation.fromNamespaceAndPath("millenaire", "harvest_sugar_cane"), "minecraft:sugar_cane"
    );

    private MillenaireAnomalyAnalyzer() {}

    static ProducerIndex buildProducerIndex(GoalRegistry goalRegistry, List<IMillCraftingRecipe> allRecipes) {
        Map<ResourceLocation, Set<Item>> byCulture = new HashMap<>();
        Set<Item> global = new HashSet<>();

        Map<ResourceLocation, Set<ResourceLocation>> goalToCultures = mapGoalsToCultures();
        Map<ResourceLocation, GatheringGoal> gatheringGoalsById = new HashMap<>();

        if (goalRegistry != null) {
            List<GatheringGoal> gatheringGoals = goalRegistry.getGatheringGoals();
            if (gatheringGoals != null) {
                for (GatheringGoal gg : gatheringGoals) {
                    if (gg != null && gg.getGatheringType() != null && gg.getGatheringType().id() != null) {
                        gatheringGoalsById.put(gg.getGatheringType().id(), gg);
                    }
                    indexGatheringGoal(gg, goalToCultures, byCulture, global);
                }
            }

            List<ResourceLocation> allGoalIds = goalRegistry.getAllIds();
            if (allGoalIds != null) {
                for (ResourceLocation goalId : allGoalIds) {
                    VillagerGoal goal = goalRegistry.get(goalId);
                    if (goal instanceof SpotGatheringGoal spotGoal) {
                        indexSpotGoal(goalId, spotGoal, goalToCultures, byCulture, global);
                    }
                }
            }
        }

        if (allRecipes != null) {
            for (IMillCraftingRecipe recipe : allRecipes) {
                if (recipe == null || "take_from_building".equalsIgnoreCase(recipe.getHandlerId())) continue;
                Set<ResourceLocation> recipeCultures = resolveRecipeCultures(recipe);
                if (recipe.getOutputs() != null) {
                    for (ItemStack out : recipe.getOutputs()) {
                        if (out != null && !out.isEmpty()) {
                            addProducer(out.getItem(), recipeCultures, byCulture, global);
                        }
                    }
                }
            }
        }

        Collection<VillagerType> allVillagers = MillenaireAPIHelper.getAllVillagers();
        if (allVillagers != null) {
            for (VillagerType vt : allVillagers) {
                if (vt == null || vt.foreignMerchantStock() == null || vt.foreignMerchantStock().isEmpty()) continue;
                Set<ResourceLocation> cultures = vt.culture() != null ? Set.of(vt.culture()) : Set.of();
                for (ResourceLocation stockItemId : vt.foreignMerchantStock().keySet()) {
                    if (stockItemId == null) continue;
                    BuiltInRegistries.ITEM.getOptional(stockItemId)
                            .filter(item -> item != Items.AIR)
                            .ifPresent(item -> addProducer(item, cultures, byCulture, global));
                }
            }
        }

        return new ProducerIndex(byCulture, global, gatheringGoalsById);
    }

    static List<String> checkUnreachableInputs(IMillCraftingRecipe recipe, ProducerIndex index) {
        if (recipe == null || recipe.getHandlerId() == null) return List.of();
        if (!CHECKED_RECIPE_HANDLERS.contains(recipe.getHandlerId().toLowerCase(Locale.ROOT))) {
            return List.of();
        }
        if (recipe.getInputs() == null || recipe.getInputs().isEmpty()) {
            return List.of();
        }

        Set<ResourceLocation> cultures = resolveRecipeCultures(recipe);
        List<String> issues = new ArrayList<>();

        for (IMillCraftingRecipe.IngredientWithCount input : recipe.getInputs()) {
            if (input == null || input.ingredient() == null) continue;
            ItemStack[] alternatives = input.ingredient().getItems();
            if (alternatives.length == 0) {
                issues.add("empty_ingredient");
                continue;
            }

            if (cultures.isEmpty()) {
                boolean anyGlobal = Arrays.stream(alternatives)
                        .anyMatch(stack -> stack != null && !stack.isEmpty() && index.global().contains(stack.getItem()));
                if (!anyGlobal) {
                    issues.add(formatInputItems(alternatives) + "[global=NONE]");
                }
            } else {
                List<String> missingCultures = new ArrayList<>();
                for (ResourceLocation cultureId : cultures) {
                    Set<Item> cultureProducers = index.byCulture().getOrDefault(cultureId, Set.of());
                    boolean reachableInCulture = Arrays.stream(alternatives)
                            .anyMatch(stack -> stack != null && !stack.isEmpty() && cultureProducers.contains(stack.getItem()));
                    if (!reachableInCulture) {
                        missingCultures.add(cultureId.getPath());
                    }
                }
                if (!missingCultures.isEmpty()) {
                    boolean anyGlobal = Arrays.stream(alternatives)
                            .anyMatch(stack -> stack != null && !stack.isEmpty() && index.global().contains(stack.getItem()));
                    String scopeNote = anyGlobal
                            ? "missing_in:" + String.join(",", missingCultures)
                            : "global=NONE,cultures:" + String.join(",", missingCultures);
                    issues.add(formatInputItems(alternatives) + "[" + scopeNote + "]");
                }
            }
        }

        return issues;
    }

    static List<String> checkSemanticMismatches(IMillCraftingRecipe recipe, ProducerIndex index) {
        if (recipe == null || recipe.getHandlerId() == null) return List.of();
        if (!CHECKED_RECIPE_HANDLERS.contains(recipe.getHandlerId().toLowerCase(Locale.ROOT))) {
            return List.of();
        }
        if (recipe.getOutputs() == null || recipe.getOutputs().isEmpty()) {
            return List.of();
        }

        List<String> inputPaths = collectInputPaths(recipe);
        List<String> outputPaths = new ArrayList<>();
        Set<ResourceLocation> outputIds = new LinkedHashSet<>();
        for (ItemStack out : recipe.getOutputs()) {
            if (out == null || out.isEmpty()) continue;
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(out.getItem());
            if (rl != null) {
                outputIds.add(rl);
                outputPaths.add(rl.getPath().toLowerCase(Locale.ROOT));
            }
        }
        if (outputPaths.isEmpty()) return List.of();

        String goalPath = recipe.getGoalKey() != null
                ? recipe.getGoalKey().toLowerCase(Locale.ROOT)
                : (recipe.getId() != null ? recipe.getId().getPath().toLowerCase(Locale.ROOT) : "");

        List<String> mismatches = new ArrayList<>();

        for (String[] pair : COMPETING_MATERIAL_PAIRS) {
            String expectedToken = pair[0];
            String wrongToken = pair[1];

            boolean expectedPresent = containsToken(goalPath, expectedToken)
                    || inputPaths.stream().anyMatch(p -> containsToken(p, expectedToken));
            boolean wrongInInputsOrGoal = containsToken(goalPath, wrongToken)
                    || inputPaths.stream().anyMatch(p -> containsToken(p, wrongToken));
            boolean wrongOnlyInOutput = outputPaths.stream()
                    .anyMatch(p -> containsToken(p, wrongToken) && !containsToken(p, expectedToken));

            if (expectedPresent && !wrongInInputsOrGoal && wrongOnlyInOutput) {
                mismatches.add(String.format("material(%s->out:%s)", expectedToken, String.join(",", outputPaths)));
            }
        }

        GatheringGoal gg = recipe.getId() != null ? index.gatheringGoalsById().get(recipe.getId()) : null;
        if (gg != null && gg.getGatheringType() != null && gg.getGatheringType().buildingLimit() != null) {
            Map<String, Integer> buildingLimits = gg.getGatheringType().buildingLimit();
            if (!buildingLimits.isEmpty()) {
                for (String limitKey : buildingLimits.keySet()) {
                    ResourceLocation limitRl = ResourceLocation.tryParse(limitKey);
                    if (limitRl == null || outputIds.contains(limitRl)) continue;
                    String limitPath = limitRl.getPath().toLowerCase(Locale.ROOT);
                    if (outputPaths.stream().anyMatch(outPath -> sharesItemFamilyBoundary(limitPath, outPath))) {
                        mismatches.add(String.format("buildingLimit(%s!=out:%s)", limitRl,
                                outputIds.stream().map(ResourceLocation::toString).collect(Collectors.joining(","))));
                    }
                }
            }
        }

        return mismatches;
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> mapGoalsToCultures() {
        Map<ResourceLocation, Set<ResourceLocation>> map = new HashMap<>();
        Collection<VillagerType> allVillagers = MillenaireAPIHelper.getAllVillagers();
        if (allVillagers == null) return map;

        for (VillagerType vt : allVillagers) {
            if (vt == null || vt.culture() == null || vt.goals() == null) continue;
            for (ResourceLocation goalId : vt.goals()) {
                if (goalId != null) {
                    map.computeIfAbsent(goalId, k -> new LinkedHashSet<>()).add(vt.culture());
                }
            }
        }
        return map;
    }

    private static void indexGatheringGoal(
            GatheringGoal goal,
            Map<ResourceLocation, Set<ResourceLocation>> goalToCultures,
            Map<ResourceLocation, Set<Item>> byCulture,
            Set<Item> global
    ) {
        if (goal == null || goal.getGatheringType() == null) return;
        GatheringType type = goal.getGatheringType();
        String handlerId = type.handlerId() != null ? type.handlerId().toLowerCase(Locale.ROOT) : "";
        if ("take_from_building".equals(handlerId)) return;

        Set<ResourceLocation> cultures = new LinkedHashSet<>();
        if (goal.id() != null) cultures.addAll(goalToCultures.getOrDefault(goal.id(), Set.of()));
        if (type.id() != null) cultures.addAll(goalToCultures.getOrDefault(type.id(), Set.of()));

        JsonObject params = type.handlerParams();
        if (params != null) {
            extractItemsFromArray(params.get("outputs"), byCulture, global, cultures);
            extractItemsFromArray(params.get("loot"), byCulture, global, cultures);
            extractItemsFromArray(params.get("bonusItems"), byCulture, global, cultures);
            extractItemsFromArray(params.get("harvestItems"), byCulture, global, cultures);
            extractItemField(params.get("harvestItem"), cultures, byCulture, global);
            extractItemField(params.get("irrigationBonusCrop"), cultures, byCulture, global);
            extractItemField(params.get("item"), cultures, byCulture, global);
        }

        switch (handlerId) {
            case "chopping" -> {
                for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "cherry", "mangrove")) {
                    resolveAndAdd("minecraft:" + wood + "_log", cultures, byCulture, global);
                    resolveAndAdd("minecraft:" + wood + "_sapling", cultures, byCulture, global);
                }
                addProducer(Items.APPLE, cultures, byCulture, global);
            }
            case "shearing" -> {
                for (DyeColor color : DyeColor.values()) {
                    resolveAndAdd("minecraft:" + color.getName() + "_wool", cultures, byCulture, global);
                }
            }
            case "paddy_harvesting" -> resolveAndAdd("millenaire:rice", cultures, byCulture, global);
            case "cocoa_harvesting" -> addProducer(Items.COCOA_BEANS, cultures, byCulture, global);
            case "fishing_inuit" -> {
                addProducer(Items.COD, cultures, byCulture, global);
                addProducer(Items.BONE_BLOCK, cultures, byCulture, global);
            }
            case "slaughter" -> {
                if (params != null) {
                    String animalType = GsonHelper.getAsString(params, "animalType", "");
                    switch (animalType) {
                        case "minecraft:cow" -> {
                            addProducer(Items.BEEF, cultures, byCulture, global);
                            addProducer(Items.LEATHER, cultures, byCulture, global);
                        }
                        case "minecraft:pig" -> addProducer(Items.PORKCHOP, cultures, byCulture, global);
                        case "minecraft:sheep" -> {
                            addProducer(Items.MUTTON, cultures, byCulture, global);
                            addProducer(Items.WHITE_WOOL, cultures, byCulture, global);
                        }
                        case "minecraft:chicken" -> {
                            addProducer(Items.CHICKEN, cultures, byCulture, global);
                            addProducer(Items.FEATHER, cultures, byCulture, global);
                        }
                        case "minecraft:squid" -> addProducer(Items.INK_SAC, cultures, byCulture, global);
                    }
                }
            }
            default -> {}
        }
    }

    private static void indexSpotGoal(
            ResourceLocation goalId,
            SpotGatheringGoal spotGoal,
            Map<ResourceLocation, Set<ResourceLocation>> goalToCultures,
            Map<ResourceLocation, Set<Item>> byCulture,
            Set<Item> global
    ) {
        Set<ResourceLocation> cultures = new LinkedHashSet<>(goalToCultures.getOrDefault(goalId, Set.of()));
        String buildingTag = readStringField(spotGoal, "buildingTag");
        if (!buildingTag.isEmpty()) {
            Collection<BuildingPlanSet> allBuildings = MillenaireAPIHelper.getAllBuildings();
            if (allBuildings != null) {
                for (BuildingPlanSet building : allBuildings) {
                    if (building != null && building.culture() != null
                            && MillenaireAPIHelper.buildingHasTag(building, buildingTag)) {
                        cultures.add(building.culture());
                    }
                }
            }
        }

        Item resolved = readStockItem(spotGoal);
        if (resolved != null && resolved != Items.AIR) {
            addProducer(resolved, cultures, byCulture, global);
        }
        String fallbackId = FALLBACK_SPOT_GOAL_OUTPUTS.get(goalId);
        if (fallbackId != null) {
            resolveAndAdd(fallbackId, cultures, byCulture, global);
        }
    }

    private static Item readStockItem(SpotGatheringGoal spotGoal) {
        return SafeReflectionHelper.readSupplierField(spotGoal, "stockItem", Item.class);
    }

    private static String readStringField(Object target, String fieldName) {
        return SafeReflectionHelper.readStringField(target, fieldName);
    }

    private static void extractItemsFromArray(
            JsonElement elem,
            Map<ResourceLocation, Set<Item>> byCulture,
            Set<Item> global,
            Set<ResourceLocation> cultures
    ) {
        if (elem == null || !elem.isJsonArray()) return;
        JsonArray arr = elem.getAsJsonArray();
        for (JsonElement entry : arr) {
            if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                resolveAndAdd(entry.getAsString(), cultures, byCulture, global);
            } else if (entry.isJsonObject()) {
                JsonObject obj = entry.getAsJsonObject();
                String itemId = GsonHelper.getAsString(obj, "item", "");
                if (itemId.isEmpty()) itemId = GsonHelper.getAsString(obj, "id", "");
                resolveAndAdd(itemId, cultures, byCulture, global);
            }
        }
    }

    private static void extractItemField(
            JsonElement elem,
            Set<ResourceLocation> cultures,
            Map<ResourceLocation, Set<Item>> byCulture,
            Set<Item> global
    ) {
        if (elem != null && elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            resolveAndAdd(elem.getAsString(), cultures, byCulture, global);
        }
    }

    private static void resolveAndAdd(
            String itemId,
            Set<ResourceLocation> cultures,
            Map<ResourceLocation, Set<Item>> byCulture,
            Set<Item> global
    ) {
        if (itemId == null || itemId.isBlank() || itemId.startsWith("#")) return;
        Item item = ItemHelper.resolve(itemId);
        if (item != null && item != Items.AIR) {
            addProducer(item, cultures, byCulture, global);
        }
    }

    private static void addProducer(
            Item item,
            Set<ResourceLocation> cultures,
            Map<ResourceLocation, Set<Item>> byCulture,
            Set<Item> global
    ) {
        if (item == null || item == Items.AIR) return;
        global.add(item);
        for (ResourceLocation culture : cultures) {
            if (culture != null) {
                byCulture.computeIfAbsent(culture, k -> new HashSet<>()).add(item);
            }
        }
    }

    private static Set<ResourceLocation> resolveRecipeCultures(IMillCraftingRecipe recipe) {
        Set<ResourceLocation> cultures = new LinkedHashSet<>();
        if (recipe.getVillagerTypes() != null) {
            for (VillagerType vt : recipe.getVillagerTypes()) {
                if (vt != null && vt.culture() != null) {
                    cultures.add(vt.culture());
                }
            }
        }
        if (cultures.isEmpty() && recipe.getAssociatedBuildingIds() != null) {
            for (BuildingPlanSet building : recipe.getAssociatedBuildingIds()) {
                if (building != null && building.culture() != null) {
                    cultures.add(building.culture());
                }
            }
        }
        return cultures;
    }

    private static List<String> collectInputPaths(IMillCraftingRecipe recipe) {
        List<String> paths = new ArrayList<>();
        if (recipe.getInputs() == null) return paths;
        for (IMillCraftingRecipe.IngredientWithCount input : recipe.getInputs()) {
            if (input == null || input.ingredient() == null) continue;
            for (ItemStack stack : input.ingredient().getItems()) {
                if (stack != null && !stack.isEmpty()) {
                    ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (rl != null) paths.add(rl.getPath().toLowerCase(Locale.ROOT));
                }
            }
        }
        return paths;
    }

    private static boolean containsToken(String path, String token) {
        if (path == null || token == null || token.isEmpty()) return false;
        if (path.equals(token)) return true;
        String[] parts = path.split("_");
        for (String part : parts) {
            if (part.equals(token)) return true;
        }
        return false;
    }

    private static boolean sharesItemFamilyBoundary(String firstPath, String secondPath) {
        String[] a = firstPath.split("_");
        String[] b = secondPath.split("_");
        if (a.length == 0 || b.length == 0) return false;
        String aFirst = a[0];
        String bFirst = b[0];
        String aLast = a[a.length - 1];
        String bLast = b[b.length - 1];
        return (aFirst.length() >= 3 && aFirst.equals(bFirst))
                || (aLast.length() >= 3 && aLast.equals(bLast));
    }

    private static String formatInputItems(ItemStack[] alternatives) {
        if (alternatives.length == 0) return "empty";
        if (alternatives.length == 1) {
            return String.valueOf(BuiltInRegistries.ITEM.getKey(alternatives[0].getItem()));
        }
        String joined = Arrays.stream(alternatives)
                .limit(3)
                .map(s -> String.valueOf(BuiltInRegistries.ITEM.getKey(s.getItem())))
                .collect(Collectors.joining("/"));
        return alternatives.length > 3 ? "(" + joined + "/+" + (alternatives.length - 3) + ")" : "(" + joined + ")";
    }

    record ProducerIndex(
            Map<ResourceLocation, Set<Item>> byCulture,
            Set<Item> global,
            Map<ResourceLocation, GatheringGoal> gatheringGoalsById
    ) {}
}
