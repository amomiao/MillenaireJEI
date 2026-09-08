package com.momos.millenairejeim.jei;

/**
 * 统一管理 JEI 集成相关的语言包 Key 与回退默认文本的静态常量类。
 * <p>包含用于断言处理缺失/任意（如“缺失文化”、“缺失建筑”）的 Key，各类 JEI Category 页签标题 Key，
 * 以及各类动态 Key 模板。</p>
 */
public final class MillenaireJeiKeys {
    private MillenaireJeiKeys() {}
    /** 工艺的本地化需要自己翻译 */
    public static final String TEMPLATE_CRAFTING_TYPE = "jei.millenaire.crafting_type.%s";

    /* ========================================================================= */
    /* 1. 缺失断言 Key & 英文回退文本                                             */
    /* ========================================================================= */
    /** 文化数据缺失断言 Key */
    public static final String KEY_MISSING_CULTURE = "jei.millenaire.assertion.missing_culture";
    public static final String FALLBACK_MISSING_CULTURE = "Missing Culture";

    /** 商店数据缺失断言 Key */
    public static final String KEY_MISSING_SHOP = "jei.millenaire.assertion.missing_shop";
    public static final String FALLBACK_MISSING_SHOP = "Missing Shop";

    /** 村民类型缺失断言 Key */
    public static final String KEY_MISSING_VILLAGER = "jei.millenaire.assertion.missing_villager";
    public static final String FALLBACK_MISSING_VILLAGER = "Missing Villager";

    /** 建筑图纸集缺失断言 Key */
    public static final String KEY_MISSING_BUILDING = "jei.millenaire.assertion.missing_building";
    public static final String FALLBACK_MISSING_BUILDING = "Missing Building";

    /** Goal 目标配方缺失断言 Key */
    public static final String KEY_MISSING_GOAL = "jei.millenaire.assertion.missing_goal";
    public static final String FALLBACK_MISSING_GOAL = "Missing Goal";

    /* ========================================================================= */
    /* 2. JEI Category 页签标题 Key                                             */
    /* ========================================================================= */
    /** 村庄出售页签标题 Key */
    public static final String KEY_CATEGORY_SELL = "jei.millenaire.category.sell";
    public static final String FALLBACK_CATEGORY_SELL = "Village Selling";

    /** 村庄收购页签标题 Key */
    public static final String KEY_CATEGORY_BUY = "jei.millenaire.category.buy";
    public static final String FALLBACK_CATEGORY_BUY = "Village Buying";

    /** 千年工艺页签标题 Key */
    public static final String KEY_CATEGORY_CRAFTING = "jei.millenaire.category.crafting";
    public static final String FALLBACK_CATEGORY_CRAFTING = "Millénaire Crafting";

    /** 油漆染色页签标题 Key */
    public static final String KEY_CATEGORY_PAINTING = "jei.millenaire.category.painting";
    public static final String FALLBACK_CATEGORY_PAINTING = "Paint Dyeing";

    /* ========================================================================= */
    /* 3. 交易界面 (Trade Category GUI) 渲染 Key                                 */
    /* ========================================================================= */
    /** 次要/可选收购提示 Key */
    public static final String KEY_TRADE_OPTIONAL_BUY = "jei.millenaire.trade.optional_buy";
    public static final String FALLBACK_TRADE_OPTIONAL_BUY = "(Optional Buy)";
    /** 声望要求格式化 Key（占位符 %s 对应具体声望值） */
    public static final String KEY_TRADE_MIN_REPUTATION = "jei.millenaire.trade.min_reputation";
    public static final String FALLBACK_TRADE_MIN_REPUTATION = "Required Reputation: %s";

    /* ========================================================================= */
    /* 4. 油漆染色界面 (Paint Category GUI) 渲染 Key                             */
    /* ========================================================================= */
    /** 目标颜色格式化 Key（占位符 %s 对应具体颜色名称） */
    public static final String KEY_PAINT_TARGET_COLOR = "jei.millenaire.paint.target_color";
    public static final String FALLBACK_PAINT_TARGET_COLOR = "Target Color: %s";
    /** 油漆涂色方块分类名称 Key & 英文回退文本 */
    public static final String KEY_PAINT_PAINTED_BRICKS = "jei.millenaire.paint.painted_bricks";
    public static final String FALLBACK_PAINT_PAINTED_BRICKS = "Painted Bricks";
    // 块
    public static final String KEY_PAINT_DECORATED_BRICKS = "jei.millenaire.paint.decorated_bricks";
    public static final String FALLBACK_PAINT_DECORATED_BRICKS = "Decorated Bricks";
    // 楼梯
    public static final String KEY_PAINT_PAINTED_BRICK_STAIRS = "jei.millenaire.paint.painted_brick_stairs";
    public static final String FALLBACK_PAINT_PAINTED_BRICK_STAIRS = "Painted Brick Stairs";
    // 台阶
    public static final String KEY_PAINT_PAINTED_BRICK_SLABS = "jei.millenaire.paint.painted_brick_slabs";
    public static final String FALLBACK_PAINT_PAINTED_BRICK_SLABS = "Painted Brick Slabs";
    // 墙
    public static final String KEY_PAINT_PAINTED_BRICK_WALLS = "jei.millenaire.paint.painted_brick_walls";
    public static final String FALLBACK_PAINT_PAINTED_BRICK_WALLS = "Painted Brick Walls";

    /* ========================================================================= */
    /* 5. 工艺界面 (Crafting Category GUI) 渲染与 Tooltip Key                    */
    /* ========================================================================= */
    // [新注释] 新增工艺 GUI 文本标签前缀 Key
    /** 工艺类型标签前缀 Key */
    public static final String KEY_LABEL_CRAFTING_TYPE = "jei.millenaire.crafting.label.crafting_type";
    public static final String FALLBACK_LABEL_CRAFTING_TYPE = "Crafting Type: ";

    /** 制作村民标签前缀 Key */
    public static final String KEY_LABEL_VILLAGER = "jei.millenaire.crafting.label.villager";
    public static final String FALLBACK_LABEL_VILLAGER = "Crafting Villager: ";

    /** 关联建筑标签前缀 Key */
    public static final String KEY_LABEL_BUILDING = "jei.millenaire.crafting.label.building";
    public static final String FALLBACK_LABEL_BUILDING = "Associated Building: ";

    /** 无村民提示 Key */
    public static final String KEY_NONE_VILLAGER = "jei.millenaire.crafting.none_villager";
    public static final String FALLBACK_NONE_VILLAGER = "No Villagers";

    /** 无建筑提示 Key */
    public static final String KEY_NONE_BUILDING = "jei.millenaire.crafting.none_building";
    public static final String FALLBACK_NONE_BUILDING = "No Buildings";

    /** 人数后缀 Key (%d 代表数量) */
    public static final String KEY_COUNT_PEOPLE = "jei.millenaire.crafting.count_people";
    public static final String FALLBACK_COUNT_PEOPLE = "etc. %d villagers";

    /** 建筑处数后缀 Key (%d 代表数量) */
    public static final String KEY_COUNT_PLACES = "jei.millenaire.crafting.count_places";
    public static final String FALLBACK_COUNT_PLACES = "etc. %d places";

    /** 村民 Tooltip 标题 Key */
    public static final String KEY_TOOLTIP_VILLAGERS_HEADER = "jei.millenaire.crafting.tooltip.villagers_header";
    public static final String FALLBACK_TOOLTIP_VILLAGERS_HEADER = "§2§l【Crafting Villagers】";

    /** 建筑 Tooltip 标题 Key */
    public static final String KEY_TOOLTIP_BUILDINGS_HEADER = "jei.millenaire.crafting.tooltip.buildings_header";
    public static final String FALLBACK_TOOLTIP_BUILDINGS_HEADER = "§3§l【Associated Buildings】";

    /** Tooltip 细项列表通用格式 Key (%s 代表名称) */
    public static final String KEY_TOOLTIP_ITEM_ENTRY = "jei.millenaire.crafting.tooltip.item_entry";
    public static final String FALLBACK_TOOLTIP_ITEM_ENTRY = "§7• %s";

    /* ========================================================================= */
    /* 6. 工艺 Handler 类型英文回退文本 (Fallback)                                 */
    /* ========================================================================= */
    public static final String FALLBACK_CRAFTING_TYPE_SMELTING = "Smelting / Cooking";
    public static final String FALLBACK_CRAFTING_TYPE_MINING = "Mining / Excavation";
    public static final String FALLBACK_CRAFTING_TYPE_FISHING = "Fishing";
    public static final String FALLBACK_CRAFTING_TYPE_HARVESTING = "Farming / Harvesting";
    public static final String FALLBACK_CRAFTING_TYPE_SLAUGHTER = "Slaughter / Animal Husbandry";
    public static final String FALLBACK_CRAFTING_TYPE_BREEDING = "Animal Breeding";
    public static final String FALLBACK_CRAFTING_TYPE_SHEARING = "Animal Shearing";
    public static final String FALLBACK_CRAFTING_TYPE_DEFAULT = "Villager Crafting";
}