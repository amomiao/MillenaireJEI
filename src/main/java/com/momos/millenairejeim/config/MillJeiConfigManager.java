package com.momos.millenairejeim.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.momos.millenairejeim.util.MMLog;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * 千年 JEI 外部配置中心。
 * <p>统一管理 {@code config/millenairejeim/} 下的两个配置目录：
 * <ol>
 *     <li>{@code spot_gathering/}：{@link org.millenaire.goal.impl.SpotGatheringGoal} 的外部注入配置；</li>
 *     <li>{@code custom_goals/}：独立页签的纯外部配置型 Goal 数据源。</li>
 * </ol>
 * 首次检测到目录不存在时自动生成默认配置与说明文档，之后完整尊重用户增删改。</p>
 */
public final class MillJeiConfigManager {
    private static final String MOD_CONFIG_FOLDER = "millenairejeim";
    private static final String DIR_SPOT_GATHERING = "spot_gathering";
    private static final String DIR_CUSTOM_GOALS = "custom_goals";

    private static final Gson GSON = new GsonBuilder().create();
    private static final Object LOCK = new Object();
    private static final AtomicLong CACHE_GENERATION = new AtomicLong(0L);

    private static final Set<String> WARNED_UNINJECTED_SPOT_GOALS = ConcurrentHashMap.newKeySet();
    private static final Set<String> WARNED_INVALID_ENTRIES = ConcurrentHashMap.newKeySet();

    private static volatile Map<String, SpotGatheringConfigEntry> spotConfigCache = null;
    private static volatile List<CustomGoalConfigEntry> customGoalCache = null;

    private MillJeiConfigManager() {}

    public static Path getConfigRootDir() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            if (configDir != null) {
                return configDir.resolve(MOD_CONFIG_FOLDER);
            }
        } catch (Throwable ignored) {
        }
        return Paths.get("config", MOD_CONFIG_FOLDER);
    }

    public static Path getSpotGatheringDir() {
        return getConfigRootDir().resolve(DIR_SPOT_GATHERING);
    }

    public static Path getCustomGoalsDir() {
        return getConfigRootDir().resolve(DIR_CUSTOM_GOALS);
    }

    public static long getCacheGeneration() {
        return CACHE_GENERATION.get();
    }

    /**
     * 初始化并预生成两个配置目录的默认文件（若目录尚不存在），供模组启动期调用。
     */
    public static void ensureInitialized() {
        synchronized (LOCK) {
            ensureDirectorySeeded(getSpotGatheringDir(), MillJeiDefaultConfigs.buildSpotGatheringDefaults(), "spot_gathering");
            ensureDirectorySeeded(getCustomGoalsDir(), MillJeiDefaultConfigs.buildCustomGoalDefaults(), "custom_goals");
        }
    }

    /**
     * 清空已解析配置缓存与去重告警集合，递增版本号触发 JEI 配方插件惰性重载。
     */
    public static void clearCache() {
        synchronized (LOCK) {
            spotConfigCache = null;
            customGoalCache = null;
        }
        WARNED_UNINJECTED_SPOT_GOALS.clear();
        WARNED_INVALID_ENTRIES.clear();
        CACHE_GENERATION.incrementAndGet();
    }

    /**
     * 按 Goal ID 或 path 查找外部注入的 {@link SpotGatheringConfigEntry}。
     */
    @Nullable
    public static SpotGatheringConfigEntry findSpotConfig(@Nullable ResourceLocation goalId) {
        if (goalId == null) {
            return null;
        }
        Map<String, SpotGatheringConfigEntry> map = getSpotGatheringConfigs();
        SpotGatheringConfigEntry direct = map.get(goalId.toString().toLowerCase(Locale.ROOT));
        if (direct != null) {
            return direct;
        }
        return map.get(goalId.getPath().toLowerCase(Locale.ROOT));
    }

    public static Map<String, SpotGatheringConfigEntry> getSpotGatheringConfigs() {
        Map<String, SpotGatheringConfigEntry> existing = spotConfigCache;
        if (existing != null) {
            return existing;
        }
        synchronized (LOCK) {
            if (spotConfigCache == null) {
                Path dir = getSpotGatheringDir();
                ensureDirectorySeeded(dir, MillJeiDefaultConfigs.buildSpotGatheringDefaults(), "spot_gathering");
                Map<String, SpotGatheringConfigEntry> loaded = new LinkedHashMap<>();
                int[] entryCount = {0};
                loadJsonEntries(dir, SpotGatheringConfigEntry.class, (entry, fileName) -> {
                    if (entry.goalId == null || entry.goalId.isBlank()) {
                        warnInvalidEntryOnce(
                                fileName + "#missing_goalId",
                                "[Millenaire-JEI] 定点采集配置 '{}' 缺少必填字段 'goalId'，已跳过。",
                                "[Millenaire-JEI] Spot gathering config '{}' is missing required field 'goalId', skipped.",
                                "[Millenaire-JEI] La config spot_gathering '{}' manque le champ 'goalId', ignorée.",
                                fileName
                        );
                        return;
                    }
                    entry.sourceFile = fileName;
                    String fullKey = normalizeGoalKey(entry.goalId);
                    loaded.put(fullKey, entry);
                    ResourceLocation parsed = ResourceLocation.tryParse(fullKey);
                    if (parsed != null) {
                        loaded.putIfAbsent(parsed.getPath().toLowerCase(Locale.ROOT), entry);
                    }
                    entryCount[0]++;
                });
                spotConfigCache = Collections.unmodifiableMap(loaded);
                MMLog.info(
                        "[Millenaire-JEI] 从配置目录 {} 加载 {} 条 SpotGatheringGoal 外部配置。",
                        "[Millenaire-JEI] From config directory {}, loaded {} SpotGatheringGoal external entries.",
                        "[Millenaire-JEI] Depuis {}, {} configurations SpotGatheringGoal chargées.",
                        dir, entryCount[0]
                );
            }
            return spotConfigCache;
        }
    }

    public static List<CustomGoalConfigEntry> getCustomGoalConfigs() {
        List<CustomGoalConfigEntry> existing = customGoalCache;
        if (existing != null) {
            return existing;
        }
        synchronized (LOCK) {
            if (customGoalCache == null) {
                Path dir = getCustomGoalsDir();
                ensureDirectorySeeded(dir, MillJeiDefaultConfigs.buildCustomGoalDefaults(), "custom_goals");
                List<CustomGoalConfigEntry> loaded = new ArrayList<>();
                loadJsonEntries(dir, CustomGoalConfigEntry.class, (entry, fileName) -> {
                    entry.sourceFile = fileName;
                    if (entry.isEnabled()) {
                        loaded.add(entry);
                    }
                });
                loaded.sort(Comparator.comparingInt(CustomGoalConfigEntry::getSortOrder)
                        .thenComparing(e -> e.sourceFile != null ? e.sourceFile : ""));
                customGoalCache = Collections.unmodifiableList(loaded);
                MMLog.info(
                        "[Millenaire-JEI] 从配置目录 {} 加载 {} 条纯配置 Goal 条目。",
                        "[Millenaire-JEI] From config directory {}, loaded {} pure-config Goal entries.",
                        "[Millenaire-JEI] Depuis {}, {} entrées Goal configurées chargées.",
                        dir, loaded.size()
                );
            }
            return customGoalCache;
        }
    }

    /**
     * 当自动化检测到尚未被 {@code spot_gathering/} 注入的 {@link org.millenaire.goal.impl.SpotGatheringGoal} 时，
     * 去重输出告警日志（同步双写至主日志与 {@code logs/millenairejeim.log}）。
     */
    public static void warnUninjectedSpotGoal(
            ResourceLocation goalId,
            @Nullable String stockItemKey,
            @Nullable String buildingTag,
            int townhallLimit
    ) {
        if (goalId == null) {
            return;
        }
        String key = goalId.toString();
        if (WARNED_UNINJECTED_SPOT_GOALS.add(key)) {
            MMLog.warn(
                    "[Millenaire-JEI] 自动化检测到未注入外部配置的 SpotGatheringGoal: '{}' (反射 stockItem={}, buildingTag={}, townhallLimit={})。"
                            + "请在 {} 目录新增 {}.json 以完善 JEI 展示（当前将尝试按 stockItem 降级显示）。",
                    "[Millenaire-JEI] Auto-detected SpotGatheringGoal '{}' without external config injection "
                            + "(reflected stockItem={}, buildingTag={}, townhallLimit={}). In {}, add {}.json to customize JEI display "
                            + "(falling back to reflected stockItem if available).",
                    "[Millenaire-JEI] SpotGatheringGoal '{}' détecté automatiquement sans config externe "
                            + "(stockItem={}, buildingTag={}, townhallLimit={}). Dans {}, ajoutez {}.json pour le configurer.",
                    key,
                    stockItemKey != null && !stockItemKey.isEmpty() ? stockItemKey : "none",
                    buildingTag != null && !buildingTag.isEmpty() ? buildingTag : "none",
                    townhallLimit,
                    getSpotGatheringDir(),
                    goalId.getPath()
            );
        }
    }

    static void warnInvalidEntryOnce(String dedupKey, String zh, String en, String fr, Object... args) {
        if (dedupKey != null && WARNED_INVALID_ENTRIES.add(dedupKey)) {
            MMLog.warn(zh, en, fr, args);
        }
    }

    /**
     * 解析可选带格式化参数的 {@link Component}（支持整数 {@code %d} 与字符串 {@code %s} 占位符）。
     */
    public static Component resolveComponent(@Nullable String key, @Nullable String fallbackText, @Nullable List<String> rawArgs) {
        String safeFallback = (fallbackText != null && !fallbackText.isBlank()) ? fallbackText : (key != null ? key : "");
        Object[] parsedArgs = parseFormatArgs(rawArgs);
        if (key != null && !key.isBlank()) {
            return parsedArgs.length > 0
                    ? Component.translatableWithFallback(key, safeFallback, parsedArgs)
                    : Component.translatableWithFallback(key, safeFallback);
        }
        return Component.literal(safeFallback);
    }

    /**
     * 批量解析 Key 列表与字面文本列表为 {@link Component} 列表。
     */
    public static List<Component> resolveComponentList(@Nullable List<String> keys, @Nullable List<String> literalLines) {
        List<Component> result = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                if (key != null && !key.isBlank()) {
                    result.add(Component.translatableWithFallback(key, key));
                }
            }
        }
        if (literalLines != null) {
            for (String line : literalLines) {
                if (line != null && !line.isBlank()) {
                    result.add(Component.literal(line));
                }
            }
        }
        return result;
    }

    private static Object[] parseFormatArgs(@Nullable List<String> rawArgs) {
        if (rawArgs == null || rawArgs.isEmpty()) {
            return new Object[0];
        }
        Object[] parsed = new Object[rawArgs.size()];
        for (int i = 0; i < rawArgs.size(); i++) {
            String raw = rawArgs.get(i);
            if (raw == null) {
                parsed[i] = "";
            } else if (raw.matches("^-?\\d+$")) {
                try {
                    parsed[i] = Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    parsed[i] = raw;
                }
            } else {
                parsed[i] = raw;
            }
        }
        return parsed;
    }

    private static String normalizeGoalKey(String rawGoalId) {
        String trimmed = rawGoalId.trim().toLowerCase(Locale.ROOT);
        return trimmed.contains(":") ? trimmed : ("millenaire:" + trimmed);
    }

    private static void ensureDirectorySeeded(Path dir, Map<String, String> defaultFiles, String label) {
        try {
            if (Files.exists(dir)) {
                return;
            }
            Files.createDirectories(dir);
            int written = 0;
            for (Map.Entry<String, String> entry : defaultFiles.entrySet()) {
                Path target = dir.resolve(entry.getKey());
                Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
                written++;
            }
            MMLog.info(
                    "[Millenaire-JEI] 首次初始化配置目录 {} ({})，已生成 {} 个默认配置文件。",
                    "[Millenaire-JEI] Initialized config directory {} ({}) with {} default files.",
                    "[Millenaire-JEI] Répertoire de configuration {} ({}) initialisé avec {} fichiers par défaut.",
                    dir, label, written
            );
        } catch (Throwable t) {
            MMLog.warn(
                    "[Millenaire-JEI] 初始化配置目录 {} 失败: {}",
                    "[Millenaire-JEI] Failed to initialize config directory {}: {}",
                    "[Millenaire-JEI] Échec de l'initialisation du répertoire {} : {}",
                    dir, t.getMessage()
            );
        }
    }

    private static <T> void loadJsonEntries(Path dir, Class<T> clazz, BiConsumer<T, String> consumer) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> jsonFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();

            for (Path file : jsonFiles) {
                String fileName = file.getFileName().toString();
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    JsonElement root = JsonParser.parseString(content);
                    if (root == null || root.isJsonNull()) {
                        continue;
                    }
                    if (root.isJsonArray()) {
                        for (JsonElement elem : root.getAsJsonArray()) {
                            deserializeSingle(elem, clazz, fileName, consumer);
                        }
                    } else if (root.isJsonObject()) {
                        JsonObject obj = root.getAsJsonObject();
                        if (obj.has("entries") && obj.get("entries").isJsonArray()) {
                            JsonArray arr = obj.getAsJsonArray("entries");
                            for (JsonElement elem : arr) {
                                deserializeSingle(elem, clazz, fileName, consumer);
                            }
                        } else {
                            deserializeSingle(obj, clazz, fileName, consumer);
                        }
                    }
                } catch (Throwable t) {
                    warnInvalidEntryOnce(
                            fileName + "#parse_error",
                            "[Millenaire-JEI] 解析配置文件 '{}' 失败（已跳过）: {}",
                            "[Millenaire-JEI] Failed to parse config file '{}' (skipped): {}",
                            "[Millenaire-JEI] Échec de l'analyse de '{}' (ignoré) : {}",
                            fileName, t.getMessage()
                    );
                }
            }
        } catch (Throwable t) {
            MMLog.warn(
                    "[Millenaire-JEI] 扫描配置目录 {} 失败: {}",
                    "[Millenaire-JEI] Failed to scan config directory {}: {}",
                    "[Millenaire-JEI] Échec de l'analyse du répertoire {} : {}",
                    dir, t.getMessage()
            );
        }
    }

    private static <T> void deserializeSingle(
            JsonElement element,
            Class<T> clazz,
            String fileName,
            BiConsumer<T, String> consumer
    ) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        T parsed = GSON.fromJson(element, clazz);
        if (parsed != null) {
            consumer.accept(parsed, fileName);
        }
    }
}
