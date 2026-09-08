package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.jei.crafting.type.base.AbstractMillCraftingType;
import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
import com.momos.millenairejeim.jei.crafting.type.StandardCraftingRecipe;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;

import java.util.ArrayList;
import java.util.List;

public final class MillCraftingRecipe {
    private static final List<AbstractMillCraftingType> PARSERS = new ArrayList<>();

    static {
        // [新注释] 注册标准手工与冶炼/烹饪配方解析器（适配 "crafting", "smelting", "standard_crafting" 等）
        registerParser(new StandardCraftingRecipe.Parser());
        // [新注释] 后续如有新的配方类型（如 GatheringRecipe, BuildingCraftingRecipe 等），直接在此注册对应的 Parser 即可：
        // registerParser(new GatheringRecipe.Parser());
    }

    private MillCraftingRecipe() {}

    public static void registerParser(AbstractMillCraftingType parser) {
        if (parser != null && !PARSERS.contains(parser)) {
            PARSERS.add(parser);
        }
    }

    public static IMillRecipe parse(GatheringType type, List<VillagerType> villagers) {
        if (type == null) return null;
        String handlerId = type.handlerId();
        for (AbstractMillCraftingType parser : PARSERS) {
            if (parser.supports(handlerId, type)) {
                IMillRecipe recipe = parser.parse(type, villagers);
                if (recipe != null) {
                    return recipe;
                }
            }
        }
        return null;
    }
}