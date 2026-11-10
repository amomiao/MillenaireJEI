package com.momos.millenairejeim.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * 模组三语（中文、英文、法文）控制台日志工具类。
 * <p>脱离 Minecraft 渲染层文本系统，在 JVM 层根据全局设置选择对应语言模板交由 {@link Logger} 输出。</p>
 * @author Momos
 * @see org.slf4j.Logger
 */
public final class MMLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("Millenaire-JEI");

    /**
     * 日志输出语言枚举。
     */
    public enum LogLanguage {
        ZH_CN,
        EN_US,
        FR_FR
    }

    /** 当前全局日志语言设置，默认优先使用英文 */
    private static LogLanguage currentLanguage = LogLanguage.EN_US;

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
     * 输出 DEBUG 级别的三语日志。
     * @param zhMessage 中文日志模板
     * @param enMessage 英文日志模板
     * @param frMessage 法文日志模板
     * @param args 参数列表（对应 SLF4J 占位符 {@code {}}）
     */
    public static void debug(String zhMessage, String enMessage, String frMessage, Object... args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(selectTemplate(zhMessage, enMessage, frMessage), args);
        }
    }

    /**
     * 输出 INFO 级别的三语日志。
     * @param zhMessage 中文日志模板
     * @param enMessage 英文日志模板
     * @param frMessage 法文日志模板
     * @param args 参数列表（对应 SLF4J 占位符 {@code {}}）
     */
    public static void info(String zhMessage, String enMessage, String frMessage, Object... args) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(selectTemplate(zhMessage, enMessage, frMessage), args);
        }
    }

    /**
     * 输出 WARN 级别的三语日志。
     * @param zhMessage 中文日志模板
     * @param enMessage 英文日志模板
     * @param frMessage 法文日志模板
     * @param args 参数列表（对应 SLF4J 占位符 {@code {}}）
     */
    public static void warn(String zhMessage, String enMessage, String frMessage, Object... args) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(selectTemplate(zhMessage, enMessage, frMessage), args);
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