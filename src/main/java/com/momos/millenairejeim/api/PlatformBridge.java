package com.momos.millenairejeim.api;

/**
 * 平台抽象桥接器静态访问点。
 */
public final class PlatformBridge {
    private static volatile IPlatformBridge INSTANCE = new Forge1201PlatformBridge();

    private PlatformBridge() {}

    public static IPlatformBridge get() {
        return INSTANCE;
    }

    /**
     * 允许在跨版本环境初始化时注入不同的平台桥实现。
     */
    public static void setInstance(IPlatformBridge bridge) {
        if (bridge != null) {
            INSTANCE = bridge;
        }
    }
}
