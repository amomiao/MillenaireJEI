package com.momos.millenairejeim.api;

import com.momos.millenairejeim.event.MillRecipeButtonClickEvent;
import com.momos.millenairejeim.event.MillRecipeButtonRenderEvent;
import com.momos.millenairejeim.event.MillRecipeSetSlotEvent;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge 1.21.1 平台桥接实现。
 */
public class NeoForge1211PlatformBridge implements IPlatformBridge {

    @Override
    public ItemStack copyWithCount(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    @Override
    public Component translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    @Override
    public Component translatableWithFallback(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(key, fallback, args);
    }

    @Override
    public <T> void postRecipeButtonClick(T recipe, String buttonId, double mouseX, double mouseY, int mouseButton) {
        NeoForge.EVENT_BUS.post(new MillRecipeButtonClickEvent<>(recipe, buttonId, mouseX, mouseY, mouseButton));
    }

    @Override
    public <T> boolean postRecipeButtonRender(T recipe, String buttonId, boolean defaultShouldRender) {
        MillRecipeButtonRenderEvent<T> event = new MillRecipeButtonRenderEvent<>(recipe, buttonId, defaultShouldRender);
        event.setShouldRender(defaultShouldRender);
        NeoForge.EVENT_BUS.post(event);
        return event.isShouldRender();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void postRecipeSetSlot(Object builder, T recipe, Object focuses) {
        if (builder instanceof IRecipeLayoutBuilder layoutBuilder && focuses instanceof IFocusGroup focusGroup) {
            NeoForge.EVENT_BUS.post(new MillRecipeSetSlotEvent<>(layoutBuilder, recipe, focusGroup));
        }
    }

    @Override
    public void playButtonClickSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
