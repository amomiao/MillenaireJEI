package com.momos.millenairejeim.util;

import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 模组三语（中文、英文、法文）双写日志工具类。
 * <p>同时输出至游戏主日志 ({@link Logger}) 与独立的 {@code logs/millenairejeim.log} 专属日志文件，
 * 脱离 Minecraft 渲染层文本系统，在 JVM 层根据全局设置选择对应语言模板。</p>
 * @author Momos
 * @see org.slf4j.Logger
 */
public final class MMLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("Millenaire-JEI");
    private static final String LOG_FILE_NAME = "millenairejeim.log";
    private static final DateTimeFormatter LINE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Object FILE_LOCK = new Object();

    private static volatile boolean sessionInitialized = false;
    private static volatile boolean fileWriteWarned = false;

    /**
     * 日志输出语言枚举。
     */
    public enum LogLanguage {
        ZH_CN,
        EN_US,
        FR_FR
    }

    /** 当前全局日志语言设置，默认优先使用英文 */
    private static volatile LogLanguage currentLanguage = LogLanguage.EN_US;

    private MMLog() {}

    /**
     * 根据 Minecraft 语言代码（如 "zh_cn", "en_us", "fr_fr", "fr_ca"）更新日志语言设置。
     *
     * @param langCode 客户端传入的语言标识字符串
     */
    public static void setLanguageFromCode(String langCode) {
        if (langCode == null || langCode.isBlank()) {
            return;
        }
        String lower = langCode.toLowerCase(Locale.ROOT);
        if (lower.startsWith("zh")) {
            currentLanguage = LogLanguage.ZH_CN;
        } else if (lower.startsWith("fr")) {
            currentLanguage = LogLanguage.FR_FR;
        } else {
            currentLanguage = LogLanguage.EN_US;
        }
    }

    /**
     * 获取当前生效的日志语言。
     * @return 当前日志语言 {@link LogLanguage}
     */
    public static LogLanguage getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * 获取专属日志文件的目标路径 ({@code logs/millenairejeim.log})。
     */
    public static Path getLogFilePath() {
        try {
            Path gameDir = FMLPaths.GAMEDIR.get();
            if (gameDir != null) {
                return gameDir.resolve("logs").resolve(LOG_FILE_NAME);
            }
        } catch (Throwable ignored) {
        }
        return Paths.get("logs", LOG_FILE_NAME);
    }

    /**
     * 输出 DEBUG 级别的三语双写日志。
     * @param zhMessage 中文日志模板
     * @param enMessage 英文日志模板
     * @param frMessage 法文日志模板
     * @param args 参数列表（对应 SLF4J 占位符 {@code {}}）
     */
    public static void debug(String zhMessage, String enMessage, String frMessage, Object... args) {
        if (LOGGER.isDebugEnabled()) {
            String template = selectTemplate(zhMessage, enMessage, frMessage);
            LOGGER.debug(template, args);
            writeToDedicatedFile("DEBUG", template, args);
        }
    }

    /**
     * 输出 INFO 级别的三语双写日志。
     * @param zhMessage 中文日志模板
     * @param enMessage 英文日志模板
     * @param frMessage 法文日志模板
     * @param args 参数列表（对应 SLF4J 占位符 {@code {}}）
     */
    public static void info(String zhMessage, String enMessage, String frMessage, Object... args) {
        String template = selectTemplate(zhMessage, enMessage, frMessage);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(template, args);
        }
        writeToDedicatedFile("INFO", template, args);
    }

    /**
     * 输出 WARN 级别的三语双写日志。
     * @param zhMessage 中文日志模板
     * @param enMessage 英文日志模板
     * @param frMessage 法文日志模板
     * @param args 参数列表（对应 SLF4J 占位符 {@code {}}）
     */
    public static void warn(String zhMessage, String enMessage, String frMessage, Object... args) {
        String template = selectTemplate(zhMessage, enMessage, frMessage);
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(template, args);
        }
        writeToDedicatedFile("WARN", template, args);
    }

    /**
     * 输出 ERROR 级别的三语双写日志。
     */
    public static void error(String zhMessage, String enMessage, String frMessage, Object... args) {
        String template = selectTemplate(zhMessage, enMessage, frMessage);
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(template, args);
        }
        writeToDedicatedFile("ERROR", template, args);
    }

    private static void writeToDedicatedFile(String level, String template, Object[] args) {
        try {
            FormattingTuple tuple = MessageFormatter.arrayFormat(template, args);
            String message = tuple.getMessage() != null ? tuple.getMessage() : String.valueOf(template);
            String timestamp = LocalDateTime.now().format(LINE_TIME_FORMAT);

            StringBuilder entry = new StringBuilder();
            entry.append('[').append(timestamp).append("] [").append(level).append("] ").append(message).append('\n');
            Throwable throwable = tuple.getThrowable();
            if (throwable != null) {
                StringWriter sw = new StringWriter();
                throwable.printStackTrace(new PrintWriter(sw));
                entry.append(sw);
                if (!sw.toString().endsWith("\n")) {
                    entry.append('\n');
                }
            }

            synchronized (FILE_LOCK) {
                Path logFile = getLogFilePath();
                if (logFile.getParent() != null) {
                    Files.createDirectories(logFile.getParent());
                }
                if (!sessionInitialized) {
                    String header = "================================================================================\n"
                            + "Millenaire-JEI Dedicated Log Session Started: " + timestamp + "\n"
                            + "================================================================================\n";
                    Files.writeString(
                            logFile,
                            header + entry,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    );
                    sessionInitialized = true;
                } else {
                    Files.writeString(
                            logFile,
                            entry.toString(),
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND
                    );
                }
            }
        } catch (Throwable t) {
            if (!fileWriteWarned) {
                fileWriteWarned = true;
                LOGGER.warn("[Millenaire-JEI] Failed to write dedicated log file '{}': {}", LOG_FILE_NAME, t.getMessage());
            }
        }
    }

    /** 根据 {@link #currentLanguage} 路由选择目标语言模板。*/
    private static String selectTemplate(String zh, String en, String fr) {
        return switch (currentLanguage) {
            case FR_FR -> fr;
            case EN_US -> en;
            default -> zh;
        };
    }
}
