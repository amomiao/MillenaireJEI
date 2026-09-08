package com.momos.millenairejeim.jei.crafting;

import com.momos.millenairejeim.jei.crafting.type.GoalEntityInteractionRecipe;
import com.momos.millenairejeim.jei.crafting.type.GoalHarvestRecipe;
import com.momos.millenairejeim.jei.crafting.type.GoalLootRecipe;
import com.momos.millenairejeim.jei.crafting.type.base.AbstractMillCraftingType;
import com.momos.millenairejeim.jei.crafting.type.base.IMillRecipe;
import com.momos.millenairejeim.jei.crafting.type.GoalCraftingRecipe;
import org.millenaire.culture.VillagerType;
import org.millenaire.goal.gathering.GatheringType;

import java.util.ArrayList;
import java.util.List;

public final class MillCraftingRecipe {
    private static final List<AbstractMillCraftingType> PARSERS = new ArrayList<>();

    static {
        // 注册标准手工与冶炼/烹饪配方解析器（适配 "crafting", "smelting", "standard_crafting" 等）
        registerParser(new GoalCraftingRecipe.Parser());
        // 注册战利品与采掘类配方解析器（适配 "mining", "fishing", "fishing_inuit" 等）
        registerParser(new GoalLootRecipe.Parser());
        // 注册农林作物与果树收割配方解析器（适配 "harvesting", "fruit_harvesting", "cocoa_harvesting" 等）
        registerParser(new GoalHarvestRecipe.Parser());
        // 注册实体交互与牧业配方解析器（适配 "slaughter", "breeding", "shearing" 等）
        registerParser(new GoalEntityInteractionRecipe.Parser());
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