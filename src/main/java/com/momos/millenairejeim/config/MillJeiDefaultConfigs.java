package com.momos.millenairejeim.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部配置文件首次生成时的默认模板工厂。
 * <p>同时维护：
 * <ul>
 *     <li>{@code spot_gathering/}：{@link org.millenaire.goal.impl.SpotGatheringGoal} 定点采集/晒干目标默认配置；</li>
 *     <li>{@code custom_goals/}：经评估具备 JEI 物品/机制查询价值的剩余 Goal（种植、造林、园艺、建筑调拨及核心内置工具/捡拾/炉灶/修路目标）。</li>
 * </ul>
 */
final class MillJeiDefaultConfigs {
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private MillJeiDefaultConfigs() {}

    static Map<String, String> buildSpotGatheringDefaults() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("_README.txt", """
                ================================================================================
                Millenaire JEI (millenairejeim) — Spot Gathering Config Directory
                ================================================================================
                All *.json files in this directory configure SpotGatheringGoal recipes
                displayed in the "Millenaire Spot Gathering" JEI tab.

                Schema (each file may be a single JSON object OR { "entries": [ ... ] }):
                - enabled (boolean, default true): set false to hide without warning logs.
                - goalId (string): Goal ResourceLocation, e.g. "millenaire:dry_brick".
                - inputs (array): [{ "item": "...", "tag": "...", "count": 1 }]
                - catalysts (array): [{ "item": "...", "count": 1 }]
                - outputs (array): [{ "item": "...", "count": 1, "tooltipKeys": [], "tooltips": [] }]
                - useReflectedStockItemFallback (boolean, default true): use reflected SpotGatheringGoal.stockItem when outputs is empty.
                - buildingTagOverride (string, optional): overrides reflected buildingTag.
                - townhallLimitOverride (int, optional): overrides reflected townhallLimit.
                - extraInfoKey / extraInfoArgs / extraInfoText: info line above villager summary.
                - extraTooltipKeys / extraTooltips: tooltips shown on hovering the extra info line.
                - outputTooltipKeys / outputTooltips: tooltips attached to all outputs of this entry.

                If Millenaire adds a new SpotGatheringGoal not listed here, Millenaire-JEI will
                log a warning to both the main log and logs/millenairejeim.log so you can add a JSON here.
                """);

        files.put("gather_silk.json", PRETTY_GSON.toJson(spotEntry(
                "millenaire:gather_silk",
                List.of(GoalItemConfigSpec.ofItem("millenaire:silk_worm", 1)),
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("millenaire:silk", 1)),
                "jei.millenaire.crafting.info.ripe_stage", List.of("3"),
                List.of(), List.of()
        )));

        files.put("gather_snails.json", PRETTY_GSON.toJson(spotEntry(
                "millenaire:gather_snails",
                List.of(GoalItemConfigSpec.ofItem("millenaire:snail_soil", 1)),
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:purple_dye", 1)),
                "jei.millenaire.crafting.info.ripe_stage", List.of("3"),
                List.of(), List.of()
        )));

        files.put("gather_brick.json", PRETTY_GSON.toJson(spotEntry(
                "millenaire:gather_brick",
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("millenaire:mud_brick", 1)),
                List.of(GoalItemConfigSpec.ofItem("millenaire:mud_brick", 1)),
                "", List.of(),
                List.of(), List.of()
        )));

        files.put("dry_brick.json", PRETTY_GSON.toJson(spotEntry(
                "millenaire:dry_brick",
                List.of(GoalItemConfigSpec.ofItem("millenaire:wet_brick", 1)),
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("millenaire:mud_brick", 1)),
                "jei.millenaire.crafting.info.spot_sun_dry", List.of(),
                List.of("jei.millenaire.crafting.tooltip.spot_sun_dry"),
                List.of("jei.millenaire.crafting.tooltip.spot_sun_dry")
        )));

        files.put("harvest_sugar_cane.json", PRETTY_GSON.toJson(spotEntry(
                "millenaire:harvest_sugar_cane",
                List.of(GoalItemConfigSpec.ofItem("minecraft:sugar_cane", 1)),
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:sugar_cane", 1)),
                "", List.of(),
                List.of(), List.of()
        )));

        files.put("plant_sugar_cane.json", PRETTY_GSON.toJson(spotEntry(
                "millenaire:plant_sugar_cane",
                List.of(GoalItemConfigSpec.ofItem("minecraft:sugar_cane", 1)),
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:sugar_cane", 1)),
                "jei.millenaire.crafting.info.spot_plant", List.of(),
                List.of("jei.millenaire.crafting.tooltip.spot_plant"),
                List.of("jei.millenaire.crafting.tooltip.spot_plant")
        )));

        return files;
    }

    static Map<String, String> buildCustomGoalDefaults() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("_README.txt", """
                ================================================================================
                Millenaire JEI (millenairejeim) — Pure Config-Driven Goals Directory
                ================================================================================
                All *.json files in this directory are loaded EXCLUSIVELY from external config
                and displayed in the dedicated "Millenaire Configured Goals" JEI tab.

                Curated defaults cover remaining Millenaire goals with concrete item/mechanics value:
                - Crop Planting (planting), Paddy Planting (paddy_planting), Cocoa Planting (cocoa_planting)
                - Sapling & Orchard Planting (sapling_planting), Flower Gardening (flower_planting)
                - Building Stock Pickup (take_from_building)
                - Useful built-in villager item/world goals: gather_goods, get_tool, light_hearth, build_path
                Pure AI/social/idle goals (idle, rest, chat, socialise, pray, cosmetic hold items) are excluded
                by default, but you may add or customize any goal JSON here freely.

                Schema (single JSON object OR { "entries": [ ... ] }):
                - enabled (boolean, default true)
                - id (string): unique recipe id, e.g. "millenaire:custom_plant_wheat"
                - goalKey (string): goal key used for default localization fallback
                - goalIds (array<string>): one or more Millenaire goal IDs used to auto-bind villagers/buildings
                - handlerId (string): label key suffix under jei.millenaire.crafting_type.<handlerId>
                - titleKey / title (string): optional localized key and fallback/literal display title
                - inputs / catalysts / outputs: item/tag specs [{ "item": "...", "tag": "...", "count": 1, "tooltipKeys": [], "tooltips": [] }]
                - extraInfoKey / extraInfoArgs / extraInfo: info line rendered under item slots
                - extraTooltipKeys / extraTooltips: tooltips shown on hovering the info line
                - villagerIds / villagerSummary / buildingIds / buildingTags / buildingSummary
                - autoBindGoalVillagers (boolean, default true)
                - autoBindBuildingsFromVillagers (boolean, default true)
                - sortOrder (int, default 100)
                """);

        // 1~9. Crop Planting (Handler: planting)
        putCustom(files, "01_plant_wheat.json", cropPlanting(
                "millenaire:custom_plant_wheat", "plant_wheat",
                List.of("millenaire:plant_wheat", "millenaire:plant_wheat_home", "millenaire:plant_wheat_paddy"),
                "jei.millenaire.config_goal.plant_wheat", "Plant Wheat",
                "minecraft:wheat_seeds", "minecraft:wheat",
                List.of("wheat", "wheatpaddy"), 10
        ));
        putCustom(files, "02_plant_carrot.json", cropPlanting(
                "millenaire:custom_plant_carrot", "plant_carrot",
                List.of("millenaire:plant_carrot", "millenaire:plant_carrot_home"),
                "jei.millenaire.config_goal.plant_carrot", "Plant Carrots",
                "minecraft:carrot", "minecraft:carrot",
                List.of("carrot"), 20
        ));
        putCustom(files, "03_plant_potato.json", cropPlanting(
                "millenaire:custom_plant_potato", "plant_potato",
                List.of("millenaire:plant_potato", "millenaire:plant_potato_home"),
                "jei.millenaire.config_goal.plant_potato", "Plant Potatoes",
                "minecraft:potato", "minecraft:potato",
                List.of("potato"), 30
        ));
        putCustom(files, "04_plant_pumpkin.json", cropPlanting(
                "millenaire:custom_plant_pumpkin", "plant_pumpkin",
                List.of("millenaire:plant_pumpkin"),
                "jei.millenaire.config_goal.plant_pumpkin", "Plant Pumpkins",
                "minecraft:pumpkin_seeds", "minecraft:pumpkin",
                List.of("pumpkins"), 40
        ));
        putCustom(files, "05_plant_cotton.json", cropPlanting(
                "millenaire:custom_plant_cotton", "plant_cotton",
                List.of("millenaire:plant_cotton", "millenaire:plant_cotton_home"),
                "jei.millenaire.config_goal.plant_cotton", "Plant Cotton",
                "millenaire:cotton", "millenaire:cotton",
                List.of("cottonfield"), 50
        ));
        putCustom(files, "06_plant_maize.json", cropPlanting(
                "millenaire:custom_plant_maize", "plant_maize",
                List.of("millenaire:plant_maize"),
                "jei.millenaire.config_goal.plant_maize", "Plant Maize",
                "millenaire:maize", "millenaire:maize",
                List.of("maizefarm"), 60
        ));
        putCustom(files, "07_plant_millet.json", cropPlanting(
                "millenaire:custom_plant_millet", "plant_millet",
                List.of("millenaire:plant_millet"),
                "jei.millenaire.config_goal.plant_millet", "Plant Millet",
                "millenaire:millet", "millenaire:millet",
                List.of("milletfarm"), 70
        ));
        putCustom(files, "08_plant_turmeric.json", cropPlanting(
                "millenaire:custom_plant_turmeric", "plant_turmeric",
                List.of("millenaire:plant_turmeric"),
                "jei.millenaire.config_goal.plant_turmeric", "Plant Turmeric",
                "millenaire:turmeric", "millenaire:turmeric",
                List.of("spicegarden"), 80
        ));
        putCustom(files, "09_plant_vines.json", cropPlanting(
                "millenaire:custom_plant_vines", "plant_vines",
                List.of("millenaire:plant_vines"),
                "jei.millenaire.config_goal.plant_vines", "Plant Grape Vines",
                "millenaire:grapes", "millenaire:grapes",
                List.of("vineyard"), 90
        ));

        // 10. Paddy Planting
        putCustom(files, "10_plant_rice.json", customEntry(
                "millenaire:custom_plant_rice", "plant_rice",
                List.of("millenaire:plant_rice"), "paddy_planting",
                "jei.millenaire.config_goal.plant_rice", "Plant Rice",
                List.of(GoalItemConfigSpec.ofItem("millenaire:rice", 1)),
                List.of(GoalItemConfigSpec.ofItem("millenaire:rice_paddy", 1)),
                List.of(GoalItemConfigSpec.ofItem("millenaire:rice", 1, "jei.millenaire.config_goal.tip.paddy_planting")),
                "jei.millenaire.config_goal.info.paddy_planting",
                List.of("jei.millenaire.config_goal.tip.paddy_planting"),
                List.of("paddy"), 100
        ));

        // 11. Cocoa Planting
        putCustom(files, "11_plant_cocoa.json", customEntry(
                "millenaire:custom_plant_cocoa", "plant_cocoa",
                List.of("millenaire:plant_cocoa"), "cocoa_planting",
                "jei.millenaire.config_goal.plant_cocoa", "Plant Cocoa",
                List.of(GoalItemConfigSpec.ofItem("minecraft:cocoa_beans", 1)),
                List.of(GoalItemConfigSpec.ofItem("minecraft:jungle_log", 1)),
                List.of(GoalItemConfigSpec.ofItem("minecraft:cocoa_beans", 1, "jei.millenaire.config_goal.tip.cocoa_planting")),
                "jei.millenaire.config_goal.info.cocoa_planting",
                List.of("jei.millenaire.config_goal.tip.cocoa_planting"),
                List.of(), 110
        ));

        // 12~15. Sapling & Orchard Planting
        putCustom(files, "12_plant_saplings_grove.json", customEntry(
                "millenaire:custom_plant_saplings_grove", "plant_saplings",
                List.of("millenaire:plant_saplings"), "sapling_planting",
                "jei.millenaire.config_goal.plant_saplings_grove", "Plant Grove Saplings",
                List.of(GoalItemConfigSpec.ofTag("minecraft:saplings", 1)),
                List.of(),
                List.of(
                        GoalItemConfigSpec.ofItem("minecraft:oak_sapling", 1),
                        GoalItemConfigSpec.ofItem("minecraft:spruce_sapling", 1),
                        GoalItemConfigSpec.ofItem("minecraft:dark_oak_sapling", 1),
                        GoalItemConfigSpec.ofItem("minecraft:birch_sapling", 1),
                        GoalItemConfigSpec.ofItem("minecraft:jungle_sapling", 1),
                        GoalItemConfigSpec.ofItem("minecraft:acacia_sapling", 1)
                ),
                "jei.millenaire.config_goal.info.sapling_planting",
                List.of("jei.millenaire.config_goal.tip.sapling_planting"),
                List.of("grove"), 120
        ));
        putCustom(files, "13_plant_olive_saplings.json", customEntry(
                "millenaire:custom_plant_olive_saplings", "plant_olive_saplings",
                List.of("millenaire:plant_olive_saplings"), "sapling_planting",
                "jei.millenaire.config_goal.plant_olive_saplings", "Plant Olive Saplings",
                List.of(GoalItemConfigSpec.ofItem("millenaire:olive_tree_sapling", 1)),
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("millenaire:olives", 1, "jei.millenaire.config_goal.tip.orchard_sapling")),
                "jei.millenaire.config_goal.info.sapling_planting",
                List.of("jei.millenaire.config_goal.tip.orchard_sapling"),
                List.of("olives"), 130
        ));
        putCustom(files, "14_plant_apple_saplings.json", customEntry(
                "millenaire:custom_plant_apple_saplings", "plant_sapling_appletree_orchard",
                List.of("millenaire:plant_sapling_appletree_orchard", "millenaire:plant_sapling_appletree_home"), "sapling_planting",
                "jei.millenaire.config_goal.plant_apple_saplings", "Plant Apple Tree Saplings",
                List.of(GoalItemConfigSpec.ofItem("millenaire:apple_tree_sapling", 1)),
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("millenaire:cider_apple", 1, "jei.millenaire.config_goal.tip.orchard_sapling")),
                "jei.millenaire.config_goal.info.sapling_planting",
                List.of("jei.millenaire.config_goal.tip.orchard_sapling"),
                List.of("lumbermanorchard"), 140
        ));
        putCustom(files, "15_plant_pistachio_saplings.json", customEntry(
                "millenaire:custom_plant_pistachio_saplings", "plant_sapling_pistachio_orchard",
                List.of("millenaire:plant_sapling_pistachio_orchard"), "sapling_planting",
                "jei.millenaire.config_goal.plant_pistachio_saplings", "Plant Pistachio Saplings",
                List.of(GoalItemConfigSpec.ofItem("millenaire:pistachio_tree_sapling", 1)),
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("millenaire:pistachios", 1, "jei.millenaire.config_goal.tip.orchard_sapling")),
                "jei.millenaire.config_goal.info.sapling_planting",
                List.of("jei.millenaire.config_goal.tip.orchard_sapling"),
                List.of("lumbermanorchard"), 150
        ));

        // 16~17. Flower Gardening
        putCustom(files, "16_plant_flowers_bonemeal.json", customEntry(
                "millenaire:custom_plant_flowers_bonemeal", "plant_norman_flowers",
                List.of("millenaire:plant_norman_flowers", "millenaire:plant_flowers_yellow", "millenaire:plant_rosebush"), "flower_planting",
                "jei.millenaire.config_goal.plant_flowers_bonemeal", "Plant Flowers (Bone Meal)",
                List.of(GoalItemConfigSpec.ofItem("minecraft:bone_meal", 1)),
                List.of(),
                List.of(
                        GoalItemConfigSpec.ofItem("minecraft:poppy", 1),
                        GoalItemConfigSpec.ofItem("minecraft:blue_orchid", 1),
                        GoalItemConfigSpec.ofItem("minecraft:dandelion", 1),
                        GoalItemConfigSpec.ofItem("minecraft:sunflower", 1),
                        GoalItemConfigSpec.ofItem("minecraft:rose_bush", 1)
                ),
                "jei.millenaire.config_goal.info.flower_bonemeal",
                List.of("jei.millenaire.config_goal.tip.flower_bonemeal"),
                List.of(), 160
        ));
        putCustom(files, "17_gardening_flowers.json", customEntry(
                "millenaire:custom_gardening_flowers", "gardening_byz",
                List.of("millenaire:gardening_byz", "millenaire:gardening_norman", "millenaire:gardening_seljuk", "millenaire:plant_indian_flowers"), "flower_planting",
                "jei.millenaire.config_goal.gardening_flowers", "Garden Flower Cultivation",
                List.of(),
                List.of(
                        GoalItemConfigSpec.ofItem("minecraft:wheat_seeds", 1),
                        GoalItemConfigSpec.ofItem("minecraft:water_bucket", 1)
                ),
                List.of(
                        GoalItemConfigSpec.ofItem("minecraft:red_tulip", 1),
                        GoalItemConfigSpec.ofItem("minecraft:pink_tulip", 1),
                        GoalItemConfigSpec.ofItem("minecraft:white_tulip", 1),
                        GoalItemConfigSpec.ofItem("minecraft:orange_tulip", 1),
                        GoalItemConfigSpec.ofItem("minecraft:poppy", 1),
                        GoalItemConfigSpec.ofItem("minecraft:blue_orchid", 1)
                ),
                "jei.millenaire.config_goal.info.gardening_flowers",
                List.of("jei.millenaire.config_goal.tip.gardening_flowers"),
                List.of("garden", "flowerfield"), 170
        ));

        // 18~21. Building Stock Transfer (take_from_building)
        putCustom(files, "18_gather_from_brick_kiln.json", customEntry(
                "millenaire:custom_gather_from_brick_kiln", "gather_from_brick_kiln",
                List.of("millenaire:gather_from_brick_kiln"), "take_from_building",
                "jei.millenaire.config_goal.gather_from_brick_kiln", "Collect Painted Bricks",
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:furnace", 1)),
                List.of(GoalItemConfigSpec.ofItem("millenaire:painted_brick_white", 1, "jei.millenaire.config_goal.tip.take_brick_kiln")),
                "jei.millenaire.config_goal.info.take_from_building",
                List.of("jei.millenaire.config_goal.tip.take_brick_kiln"),
                List.of("brickkiln"), 180
        ));
        putCustom(files, "19_fetch_bread_byz.json", customEntry(
                "millenaire:custom_fetch_bread_byz", "fetch_bread_byz",
                List.of("millenaire:fetch_bread_byz"), "take_from_building",
                "jei.millenaire.config_goal.fetch_bread_byz", "Fetch Byzantine Bread",
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:wheat", 1)),
                List.of(GoalItemConfigSpec.ofItem("minecraft:bread", 1, "jei.millenaire.config_goal.tip.fetch_bread_byz")),
                "jei.millenaire.config_goal.info.take_from_building",
                List.of("jei.millenaire.config_goal.tip.fetch_bread_byz"),
                List.of("bakery"), 190
        ));
        putCustom(files, "20_fetch_iron_byz.json", customEntry(
                "millenaire:custom_fetch_iron_byz", "fetch_iron_byz",
                List.of("millenaire:fetch_iron_byz"), "take_from_building",
                "jei.millenaire.config_goal.fetch_iron_byz", "Fetch Byzantine Iron",
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:blast_furnace", 1)),
                List.of(GoalItemConfigSpec.ofItem("minecraft:iron_ingot", 1, "jei.millenaire.config_goal.tip.fetch_iron_byz")),
                "jei.millenaire.config_goal.info.take_from_building",
                List.of("jei.millenaire.config_goal.tip.fetch_iron_byz"),
                List.of("centralworkshop"), 200
        ));
        putCustom(files, "21_fetch_sandstone_byz.json", customEntry(
                "millenaire:custom_fetch_sandstone_byz", "fetch_sandstone_byz",
                List.of("millenaire:fetch_sandstone_byz"), "take_from_building",
                "jei.millenaire.config_goal.fetch_sandstone_byz", "Fetch Byzantine Sandstone",
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:chiseled_sandstone", 1)),
                List.of(GoalItemConfigSpec.ofItem("minecraft:sandstone", 1, "jei.millenaire.config_goal.tip.fetch_sandstone_byz")),
                "jei.millenaire.config_goal.info.take_from_building",
                List.of("jei.millenaire.config_goal.tip.fetch_sandstone_byz"),
                List.of("centralworkshop"), 210
        ));

        // 22~25. Remaining high-value Java built-in Goals
        putCustom(files, "22_gather_goods.json", customEntry(
                "millenaire:custom_gather_goods", "gather_goods",
                List.of("millenaire:gather_goods"), "custom_goal",
                "jei.millenaire.config_goal.gather_goods", "Gather Ground Items",
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:chest", 1)),
                List.of(
                        GoalItemConfigSpec.ofItem("minecraft:oak_log", 1, "jei.millenaire.config_goal.tip.gather_goods"),
                        GoalItemConfigSpec.ofItem("minecraft:apple", 1, "jei.millenaire.config_goal.tip.gather_goods"),
                        GoalItemConfigSpec.ofItem("millenaire:cider_apple", 1, "jei.millenaire.config_goal.tip.gather_goods")
                ),
                "jei.millenaire.config_goal.info.gather_goods",
                List.of("jei.millenaire.config_goal.tip.gather_goods"),
                List.of(), 220
        ));
        putCustom(files, "23_get_tool.json", customEntry(
                "millenaire:custom_get_tool", "get_tool",
                List.of("millenaire:get_tool"), "custom_goal",
                "jei.millenaire.config_goal.get_tool", "Get & Upgrade Tools",
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:anvil", 1)),
                List.of(
                        GoalItemConfigSpec.ofItem("minecraft:iron_axe", 1, "jei.millenaire.config_goal.tip.get_tool"),
                        GoalItemConfigSpec.ofItem("minecraft:iron_pickaxe", 1, "jei.millenaire.config_goal.tip.get_tool"),
                        GoalItemConfigSpec.ofItem("minecraft:iron_shovel", 1, "jei.millenaire.config_goal.tip.get_tool"),
                        GoalItemConfigSpec.ofItem("minecraft:iron_sword", 1, "jei.millenaire.config_goal.tip.get_tool")
                ),
                "jei.millenaire.config_goal.info.get_tool",
                List.of("jei.millenaire.config_goal.tip.get_tool"),
                List.of(), 230
        ));
        putCustom(files, "24_light_hearth.json", customEntry(
                "millenaire:custom_light_hearth", "light_hearth",
                List.of("millenaire:light_hearth"), "custom_goal",
                "jei.millenaire.config_goal.light_hearth", "Light Home Hearth",
                List.of(GoalItemConfigSpec.ofItem("minecraft:campfire", 1)),
                List.of(GoalItemConfigSpec.ofItem("minecraft:flint_and_steel", 1)),
                List.of(GoalItemConfigSpec.ofItem("minecraft:campfire", 1, "jei.millenaire.config_goal.tip.light_hearth")),
                "jei.millenaire.config_goal.info.light_hearth",
                List.of("jei.millenaire.config_goal.tip.light_hearth"),
                List.of(), 240
        ));
        putCustom(files, "25_build_path.json", customEntry(
                "millenaire:custom_build_path", "build_path",
                List.of("millenaire:build_path"), "custom_goal",
                "jei.millenaire.config_goal.build_path", "Build Village Paths",
                List.of(),
                List.of(GoalItemConfigSpec.ofItem("minecraft:iron_shovel", 1)),
                List.of(
                        GoalItemConfigSpec.ofItem("minecraft:dirt_path", 1, "jei.millenaire.config_goal.tip.build_path"),
                        GoalItemConfigSpec.ofItem("minecraft:gravel", 1, "jei.millenaire.config_goal.tip.build_path")
                ),
                "jei.millenaire.config_goal.info.build_path",
                List.of("jei.millenaire.config_goal.tip.build_path"),
                List.of(), 250
        ));

        return files;
    }

    private static SpotGatheringConfigEntry spotEntry(
            String goalId,
            List<GoalItemConfigSpec> inputs,
            List<GoalItemConfigSpec> catalysts,
            List<GoalItemConfigSpec> outputs,
            String extraInfoKey,
            List<String> extraInfoArgs,
            List<String> extraTooltipKeys,
            List<String> outputTooltipKeys
    ) {
        SpotGatheringConfigEntry entry = new SpotGatheringConfigEntry();
        entry.enabled = true;
        entry.goalId = goalId;
        entry.inputs.addAll(inputs);
        entry.catalysts.addAll(catalysts);
        entry.outputs.addAll(outputs);
        entry.extraInfoKey = extraInfoKey;
        entry.extraInfoArgs.addAll(extraInfoArgs);
        entry.extraTooltipKeys.addAll(extraTooltipKeys);
        entry.outputTooltipKeys.addAll(outputTooltipKeys);
        return entry;
    }

    private static CustomGoalConfigEntry cropPlanting(
            String id,
            String goalKey,
            List<String> goalIds,
            String titleKey,
            String title,
            String seedItemId,
            String cropResultItemId,
            List<String> buildingTags,
            int sortOrder
    ) {
        return customEntry(
                id, goalKey, goalIds, "planting", titleKey, title,
                List.of(GoalItemConfigSpec.ofItem(seedItemId, 1)),
                List.of(GoalItemConfigSpec.ofItem("minecraft:farmland", 1)),
                List.of(GoalItemConfigSpec.ofItem(cropResultItemId, 1, "jei.millenaire.config_goal.tip.crop_planting")),
                "jei.millenaire.config_goal.info.crop_planting",
                List.of("jei.millenaire.config_goal.tip.crop_planting"),
                buildingTags,
                sortOrder
        );
    }

    private static CustomGoalConfigEntry customEntry(
            String id,
            String goalKey,
            List<String> goalIds,
            String handlerId,
            String titleKey,
            String title,
            List<GoalItemConfigSpec> inputs,
            List<GoalItemConfigSpec> catalysts,
            List<GoalItemConfigSpec> outputs,
            String extraInfoKey,
            List<String> extraTooltipKeys,
            List<String> buildingTags,
            int sortOrder
    ) {
        CustomGoalConfigEntry entry = new CustomGoalConfigEntry();
        entry.enabled = true;
        entry.id = id;
        entry.goalKey = goalKey;
        entry.goalIds.addAll(goalIds);
        entry.handlerId = handlerId;
        entry.titleKey = titleKey;
        entry.title = title;
        entry.inputs.addAll(inputs);
        entry.catalysts.addAll(catalysts);
        entry.outputs.addAll(outputs);
        entry.extraInfoKey = extraInfoKey;
        entry.extraTooltipKeys.addAll(extraTooltipKeys);
        entry.buildingTags.addAll(buildingTags);
        entry.sortOrder = sortOrder;
        return entry;
    }

    private static void putCustom(Map<String, String> files, String fileName, CustomGoalConfigEntry entry) {
        files.put(fileName, PRETTY_GSON.toJson(entry));
    }
}
