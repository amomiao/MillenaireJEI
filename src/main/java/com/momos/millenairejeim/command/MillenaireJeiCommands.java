package com.momos.millenairejeim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.momos.millenairejeim.MillenaireJeim;
import com.momos.millenairejeim.helper.MillenaireAPIHelper;
import com.momos.millenairejeim.helper.MillenaireLocalizeHelper;
import com.momos.millenairejeim.jei.crafting.MillCraftingRecipeMaker;
import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import com.momos.millenairejeim.util.MMLog;
import com.momos.millenairejeim.util.MillenaireJeimLocalizeHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.millenaire.Millenaire;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.GoalRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Millenaire JEI 诊断与日志导出命令系统。
 * 支持导出配方异常（无村民/无建筑）及全英文未本地化工艺列表，日志语言受 {@link MMLog} 统一控制。
 */
@EventBusSubscriber(modid = MillenaireJeim.MODID)
public class MillenaireJeiCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("milljei")
                .then(Commands.literal("export")
                        .then(Commands.literal("anomalies").executes(ctx -> exportAnomalies(ctx.getSource())))
                        .then(Commands.literal("english_crafts").executes(ctx -> exportEnglishCrafts(ctx.getSource())))
                        .then(Commands.literal("all").executes(ctx -> exportAll(ctx.getSource())))
                        .executes(ctx -> exportAll(ctx.getSource()))
                );

        dispatcher.register(cmd);

        // 注册常用别名
        dispatcher.register(Commands.literal("milljeim")
                .then(Commands.literal("export")
                        .then(Commands.literal("anomalies").executes(ctx -> exportAnomalies(ctx.getSource())))
                        .then(Commands.literal("english_crafts").executes(ctx -> exportEnglishCrafts(ctx.getSource())))
                        .then(Commands.literal("all").executes(ctx -> exportAll(ctx.getSource())))
                        .executes(ctx -> exportAll(ctx.getSource()))
                ));
    }

    private static int exportAll(CommandSourceStack source) {
        int r1 = exportAnomalies(source);
        int r2 = exportEnglishCrafts(source);
        return r1 + r2;
    }

    /**
     * 导出异常配方（如合成配方无村民或无建筑）。
     */
    public static int exportAnomalies(CommandSourceStack source) {
        GoalRegistry goalRegistry = Millenaire.getGoalRegistry();
        if (goalRegistry == null) {
            MMLog.warn(
                    "[Millenaire-JEI] 无法导出异常：千年村庄 GoalRegistry 尚未初始化！",
                    "[Millenaire-JEI] Cannot export anomalies: Millenaire GoalRegistry is not initialized yet!",
                    "[Millenaire-JEI] Impossible d'exporter les anomalies: le GoalRegistry Millenaire n'est pas encore initialisé!"
            );
            source.sendFailure(Component.literal("[Millenaire-JEI] GoalRegistry not ready yet!"));
            return 0;
        }

        List<IMillCraftingRecipe> allRecipes = MillCraftingRecipeMaker.initRecipes(goalRegistry);
        List<String> anomalyLines = new ArrayList<>();

        int missingVillagerCount = 0;
        int missingBuildingCount = 0;
        int missingOutputCount = 0;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        for (IMillCraftingRecipe recipe : allRecipes) {
            boolean hasNoVillagers = recipe.getVillagerTypes() == null || recipe.getVillagerTypes().isEmpty();
            boolean hasNoBuildings = recipe.getAssociatedBuildingIds() == null || recipe.getAssociatedBuildingIds().isEmpty();
            boolean hasNoOutputs = recipe.getOutputs() == null || recipe.getOutputs().isEmpty();

            if (hasNoVillagers || hasNoBuildings || hasNoOutputs) {
                StringBuilder line = new StringBuilder();
                line.append(String.format("• ID: %s | Handler: %s | GoalKey: %s",
                        recipe.getId(), recipe.getHandlerId(), recipe.getGoalKey()));

                if (hasNoVillagers) {
                    missingVillagerCount++;
                    line.append(" [MISSING_VILLAGER / 缺失村民]");
                }
                if (hasNoBuildings) {
                    missingBuildingCount++;
                    line.append(" [MISSING_BUILDING / 缺失建筑]");
                }
                if (hasNoOutputs) {
                    missingOutputCount++;
                    line.append(" [MISSING_OUTPUT / 缺失产物]");
                }
                anomalyLines.add(line.toString());
            }
        }

        // 构建三语报告文本
        List<String> reportContent = new ArrayList<>();
        reportContent.add("================================================================================");
        reportContent.add(switch (MMLog.getCurrentLanguage()) {
            case ZH_CN -> "[Millenaire-JEI 配方异常诊断报告] 生成时间: " + timestamp;
            case FR_FR -> "[Rapport d'anomalies de recettes Millenaire-JEI] Date: " + timestamp;
            case EN_US -> "[Millenaire-JEI Recipe Anomalies Diagnostic Report] Timestamp: " + timestamp;
        });
        reportContent.add(String.format("Total Recipes Scanned: %d | Total Anomalies: %d", allRecipes.size(), anomalyLines.size()));
        reportContent.add(String.format("  - Missing Villagers: %d", missingVillagerCount));
        reportContent.add(String.format("  - Missing Buildings: %d", missingBuildingCount));
        reportContent.add(String.format("  - Missing Outputs:   %d", missingOutputCount));
        reportContent.add("================================================================================");
        reportContent.addAll(anomalyLines);

        // 输出至 MMLog 日志系统
        MMLog.warn(
                "[Millenaire-JEI] 扫描到 {} 条异常配方（无村民: {}, 无建筑: {}），详情写入日志文件",
                "[Millenaire-JEI] Scanned {} recipe anomalies (no villagers: {}, no buildings: {}), details written to log",
                "[Millenaire-JEI] {} anomalies de recettes détectées (sans villageois: {}, sans bâtiment: {}), détails écrits",
                anomalyLines.size(), missingVillagerCount, missingBuildingCount
        );

        // 写入独立文件
        Path logFile = Paths.get("logs", "millenaire_jei_anomalies.log");
        writeFileSafely(logFile, reportContent);

        source.sendSuccess(() -> Component.literal(String.format(
                "§6[Millenaire-JEI] §f异常配方扫描完成: 扫描 §e%d§f 条, 异常 §c%d§f 条. 详见: §b%s",
                allRecipes.size(), anomalyLines.size(), logFile.toAbsolutePath())), true);

        return anomalyLines.size();
    }

    /**
     * 导出全英文名的工艺，方便非英文语言环境排查本地化翻译覆盖率。
     */
    public static int exportEnglishCrafts(CommandSourceStack source) {
        GoalRegistry goalRegistry = Millenaire.getGoalRegistry();
        if (goalRegistry == null) {
            MMLog.warn(
                    "[Millenaire-JEI] 无法导出全英文工艺：GoalRegistry 尚未就绪！",
                    "[Millenaire-JEI] Cannot export English crafts: GoalRegistry not ready!",
                    "[Millenaire-JEI] Impossible d'exporter les recettes: GoalRegistry non prêt!"
            );
            source.sendFailure(Component.literal("[Millenaire-JEI] GoalRegistry not ready yet!"));
            return 0;
        }

        List<IMillCraftingRecipe> allRecipes = MillCraftingRecipeMaker.initRecipes(goalRegistry);
        List<String> englishCraftLines = new ArrayList<>();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        for (IMillCraftingRecipe recipe : allRecipes) {
            String rawGoalKey = recipe.getGoalKey() != null ? recipe.getGoalKey() : (recipe.getId() != null ? recipe.getId().getPath() : "unknown");
            Component localizedGoalName = MillenaireLocalizeHelper.getGoalName(rawGoalKey);
            String goalStr = localizedGoalName.getString();

            String handlerId = recipe.getHandlerId() != null ? recipe.getHandlerId().toLowerCase() : "crafting";
            Component localizedType = MillenaireLocalizeHelper.getCraftingTypeName(handlerId);
            String typeStr = localizedType.getString();

            // 判断工艺名称或配方名称是否为纯 ASCII / 英文表示（非本地化字符）
            boolean isGoalEnglish = goalStr.matches("^[\\x00-\\x7F]+$");
            boolean isTypeEnglish = typeStr.matches("^[\\x00-\\x7F]+$");

            if (isGoalEnglish || isTypeEnglish) {
                englishCraftLines.add(String.format("• [%s] %s | ID: %s | RawGoal: %s | Handler: %s",
                        typeStr, goalStr, recipe.getId(), rawGoalKey, handlerId));
            }
        }

        List<String> reportContent = new ArrayList<>();
        reportContent.add("================================================================================");
        reportContent.add(switch (MMLog.getCurrentLanguage()) {
            case ZH_CN -> "[Millenaire-JEI 全英文/未本地化工艺排查清单] 生成时间: " + timestamp;
            case FR_FR -> "[Liste de recettes en anglais / non traduites Millenaire-JEI] Date: " + timestamp;
            case EN_US -> "[Millenaire-JEI English/Unlocalized Crafts Inventory] Timestamp: " + timestamp;
        });
        reportContent.add(String.format("Total Recipes Scanned: %d | English / Unlocalized Entries: %d", allRecipes.size(), englishCraftLines.size()));
        reportContent.add("================================================================================");
        reportContent.addAll(englishCraftLines);

        MMLog.info(
                "[Millenaire-JEI] 已排查出 {} 条全英文/未本地化工艺配方，已导出至独立清单文件",
                "[Millenaire-JEI] Found {} English/unlocalized crafting recipes, exported to inventory file",
                "[Millenaire-JEI] {} recettes en anglais / non traduites identifiées, exportées",
                englishCraftLines.size()
        );

        Path logFile = Paths.get("logs", "millenaire_jei_english_crafts.log");
        writeFileSafely(logFile, reportContent);

        source.sendSuccess(() -> Component.literal(String.format(
                "§6[Millenaire-JEI] §f全英文工艺导出完成: 共 §c%d§f 条未本地化/纯英文工艺. 详见: §b%s",
                englishCraftLines.size(), logFile.toAbsolutePath())), true);

        return englishCraftLines.size();
    }

    private static void writeFileSafely(Path path, List<String> lines) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            MMLog.warn(
                    "[Millenaire-JEI] 写入诊断文件失败: {}",
                    "[Millenaire-JEI] Failed to write diagnostic file: {}",
                    "[Millenaire-JEI] Échec de l'écriture du fichier de diagnostic: {}",
                    e.getMessage()
            );
        }
    }
}
