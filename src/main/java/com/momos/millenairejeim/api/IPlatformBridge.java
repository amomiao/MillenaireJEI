package com.momos.millenairejeim.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 跨版本平台抽象桥接契约。
 * 隔离 1.20.1 (Forge/旧版 API) 与 1.21.1 (NeoForge/现代 API) 的底层调用差异。
 */
public interface IPlatformBridge {

    /**
     * 创建安全带有指定数量的物品副本。
     */
    ItemStack copyWithCount(ItemStack stack, int count);

    /**
     * 构建国际化文本（支持带回退文本）。
     */
    Component translatable(String key, Object... args);

    /**
     * 构建带有英文回退的国际化文本。
     */
    Component translatableWithFallback(String key, String fallback, Object... args);

    /**
     * 统一派发配方按钮点击事件。
     * 1.21.1 派发至 NeoForge.EVENT_BUS，1.20.1 派发至 MinecraftForge.EVENT_BUS。
     */
    <T> void postRecipeButtonClick(T recipe, String buttonId, double mouseX, double mouseY, int mouseButton);

    /**
     * 统一派发配方按钮渲染状态检查事件。
     */
    <T> boolean postRecipeButtonRender(T recipe, String buttonId, boolean defaultShouldRender);

    /**
     * 统一派发槽位设置事件。
     */
    <T> void postRecipeSetSlot(Object builder, T recipe, Object focuses);

    /**
     * 播放标准原版 UI 按钮点击音效。
     */
    void playButtonClickSound();
}
