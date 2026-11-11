package com.momos.millenairejeim.config;

import com.momos.millenairejeim.jei.crafting.type.base.IMillCraftingRecipe;
import com.momos.millenairejeim.util.MMLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI 外部 Goal 配置中的物品/标签条目通用描述符。
 * <p>同时复用于 {@code spot_gathering} 与 {@code custom_goals} 两套配置体系，避免重复解析逻辑。</p>
 */
public class GoalItemConfigSpec {
    public String item = "";
    public String tag = "";
    public int count = 1;
    public List<String> tooltipKeys = new ArrayList<>();
    public List<String> tooltips = new ArrayList<>();

    public GoalItemConfigSpec() {}

    public static GoalItemConfigSpec ofItem(String itemId, int count) {
        GoalItemConfigSpec spec = new GoalItemConfigSpec();
        spec.item = itemId;
        spec.count = Math.max(1, count);
        return spec;
    }

    public static GoalItemConfigSpec ofItem(String itemId, int count, String tooltipKey, String... literalTooltips) {
        GoalItemConfigSpec spec = ofItem(itemId, count);
        if (tooltipKey != null && !tooltipKey.isBlank()) {
            spec.tooltipKeys.add(tooltipKey);
        }
        if (literalTooltips != null) {
            for (String line : literalTooltips) {
                if (line != null && !line.isBlank()) {
                    spec.tooltips.add(line);
                }
            }
        }
        return spec;
    }

    public static GoalItemConfigSpec ofTag(String tagId, int count) {
        GoalItemConfigSpec spec = new GoalItemConfigSpec();
        spec.tag = tagId;
        spec.count = Math.max(1, count);
        return spec;
    }

    @Nullable
    public IMillCraftingRecipe.IngredientWithCount toIngredientWithCount(String sourceFile) {
        int safeCount = Math.max(1, count);
        if (tag != null && !tag.isBlank()) {
            String normalized = tag.startsWith("#") ? tag.substring(1) : tag;
            ResourceLocation tagRl = ResourceLocation.tryParse(normalized);
            if (tagRl != null) {
                return new IMillCraftingRecipe.IngredientWithCount(
                        Ingredient.of(TagKey.create(Registries.ITEM, tagRl)), safeCount
                );
            }
            MillJeiConfigManager.warnInvalidEntryOnce(
                    sourceFile + "#tag:" + tag,
                    "[Millenaire-JEI] 物品标签 '{}' 无效（配置: '{}'），已跳过。",
                    "[Millenaire-JEI] Invalid item tag '{}' in config '{}', skipped.",
                    "[Millenaire-JEI] Tag d'objet invalide '{}' dans '{}', ignoré.",
                    tag, sourceFile
            );
            return null;
        }
        Item resolved = resolveItemStrict(item, sourceFile);
        if (resolved == null || resolved == Items.AIR) {
            return null;
        }
        return new IMillCraftingRecipe.IngredientWithCount(Ingredient.of(resolved), safeCount);
    }

    public ItemStack toItemStack(String sourceFile) {
        Item resolved = resolveItemStrict(item, sourceFile);
        if (resolved == null || resolved == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(resolved, Math.max(1, count));
    }

    public List<Component> buildTooltips() {
        List<Component> result = new ArrayList<>();
        if (tooltipKeys != null) {
            for (String key : tooltipKeys) {
                if (key != null && !key.isBlank()) {
                    result.add(Component.translatableWithFallback(key, key));
                }
            }
        }
        if (tooltips != null) {
            for (String line : tooltips) {
                if (line != null && !line.isBlank()) {
                    result.add(Component.literal(line));
                }
            }
        }
        return result;
    }

    @Nullable
    private static Item resolveItemStrict(@Nullable String rawItemId, String sourceFile) {
        if (rawItemId == null || rawItemId.isBlank()) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(rawItemId);
        if (rl != null) {
            Optional<Item> opt = BuiltInRegistries.ITEM.getOptional(rl);
            if (opt.isPresent() && opt.get() != Items.AIR) {
                return opt.get();
            }
        }
        MillJeiConfigManager.warnInvalidEntryOnce(
                sourceFile + "#item:" + rawItemId,
                "[Millenaire-JEI] 物品 ID '{}' 未在注册表中找到（配置: '{}'），已跳过。",
                "[Millenaire-JEI] Item ID '{}' in config '{}' was not found in registry, skipped.",
                "[Millenaire-JEI] L'ID d'objet '{}' dans '{}' est introuvable, ignoré.",
                rawItemId, sourceFile
        );
        MMLog.debug(
                "[Millenaire-JEI] 无效物品配置条目: {} ({})",
                "[Millenaire-JEI] Unresolved config item entry: {} ({})",
                "[Millenaire-JEI] Entrée d'objet non résolue : {} ({})",
                rawItemId, sourceFile
        );
        return null;
    }
}
