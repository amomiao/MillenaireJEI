# Human Claw Runtime
## 总之先查阅可能有用的接口
### Buy&Sell from Village 千年村庄交易
* 页签:`千年村庄出售`、`千年村庄购入`
* `org.millenaire.building.BuildingPlan`
  * `public List<Shop> shops`: 存储当前建筑包含的商店定义。可遍历获取该建筑交易的物品列表、基础收购/出售价格。
* `org.millenaire.building.BuildingPlanSet$StartingGood`
  * `public Item item`: 交易物品。
  * `public int price`: 物品基础价格（以铜钱/银钱/金钱算法换算）。
  * `public int sellPrice`: 村庄收购玩家物品的价格。
* `org.millenaire.building.GoodAvailabilityHelper`
  * `public static boolean isItemSoldAt(Culture culture, Item item)`: 便捷校验方法，用于过滤 JEI 交易配方。
### Villager Crafting 千年村庄工艺
* 页签:`千年村庄烹饪`、`千年村庄工艺`
* `org.millenaire.block.FirePitBlockEntity`
  * `public static boolean isFirePitBurnable(ItemStack stack)`: 判定火塘有效燃料/原料。
  * `public static int getCookTime(ItemStack stack)`: 提取火塘加工耗时（Ticks）。
* `org.millenaire.building.BuildingPlanSet$LevelDef`
  * `public List<InvItem> abstractedProduction`: **关键数据**。记录该等级建筑（如磨坊、面包房、铁匠铺）随时间被动生成的物品清单，用作 JEI "工坊自动产出" 配方。
* `org.millenaire.block.BlockWetBrick`
  * 湿砖风干成干砖的状态变化逻辑，对应 JEI "自然风干" 分类。
* `org.millenaire.block.BlockSilkWorm` & `org.millenaire.block.BlockSnailSoil`
  * 蚕架与蜗牛土方块的时序产出项（蚕丝、蜗牛）。
### Villager Slaugtering 千年屠宰[不做]
* 页签:`千年养殖`
* `org.millenaire.block.mock.AnimalSpawnType`
  * 枚举类：定义建筑内标记的动物类型（如 `COW`, `PIG`, `CHICKEN`, `SHEEP` 等）。
* `org.millenaire.block.mock.MockAnimalSpawnBlock`
  * 蓝图生成块：通过读取蓝图中的 `MockAnimalSpawnBlock` 绑定的 `AnimalSpawnType`，映射出“特定文化建筑 $\rightarrow$ 养殖动物 $\rightarrow$ 屠宰产出物”。
### Bucket Painting 漆刷
* 页签:`油漆染色`
* `org.millenaire.block.IPaintedBlock`
  * `public EnumColour getColor()`: 获取方块颜色。
  * `public BrickType getBrickType()`: 获取对应砖块变体。
* `org.millenaire.block.PaintedBrickBlock$BrickType`
  * 枚举类：包含所有可刷漆的方块样式。结合颜色枚举，通过双重循环动态向 JEI 注册 `[基础方块] + [特定油漆] -> [染色方块]`，无需硬编码。

## 审阅类
1. 交易模块 (Village Commerce)
   * org.millenaire.building.BuildingPlan：检查 shops 列表的包含关系与数据结构。
   * org.millenaire.building.BuildingPlanSet 及内部类 StartingGood：检查 price、sellPrice 字段类型与基础计价逻辑。
   * org.millenaire.building.GoodAvailabilityHelper：查看 isItemSoldAt 的判定方法实现。
   * org.millenaire.building.Shop（若为独立类或内部类）：检查其内部维护商品明细（Goods List）的存储方式。
     * org.millenaire.commerce.ShopProfile
   * org.millenaire.commerce.TradeGood
   * org.millenaire.commerce.TradeGoodsLoader
   * org.millenaire.commerce.ShopProfileLoader
   * org.millenaire.item.MillItems货币铜钱（Denier Bronze）、银钱（Denier Silver）、金钱（Denier Gold）的 Item 静态引用

2. 工艺与被动生产模块 (Villager Crafting)
   * org.millenaire.goal.gathering.handler.CraftingHandler
   * org.millenaire.goal.gathering.GatheringType
   * org.millenaire.goal.gathering.GatheringTypeLoader
   * org.millenaire.goal.GoalRegistry
   * org.millenaire.goal.gathering.handler.AbstractGatheringHandler
   * 
   * org.millenaire.building.BuildingPlanSet$LevelDef：重点查看 abstractedProduction 字段的数据结构及其定义方式。
   * org.millenaire.building.InvItem：查看如何将 Millénaire 的 InvItem 安全还原为标准 ItemStack（含 Count 与 NBT 处理）。
   * org.millenaire.block.FirePitBlockEntity：查看火塘配方判定函数 isFirePitBurnable 及烹饪耗时 getCookTime 的代码实现。
   * org.millenaire.block.BlockWetBrick、BlockSilkWorm、BlockSnailSoil：查看干燥、蚕丝与蜗牛产出的静态逻辑或 Tick 转化方法。

3. 漆刷染色模块 (Bucket Painting)
   * org.millenaire.block.IPaintedBlock：查看接口定义。
   * org.millenaire.block.PaintedBrickBlock 及其内部枚举 BrickType：查看变体映射与颜色枚举值。
   * 油漆/刷子对应的 Item 类（如 ItemPaintBucket 或 ItemPaintBrush）：查看漆刷物品的数据结构，以便生成 JEI 配方中的输入物品。

4. 货币与文化辅助（可选）
   * org.millenaire.culture.Culture 或 org.millenaire.item.MillItems：查看 Denier（铜钱/银钱/金钱）的物品注册与基础面额换算方法。

## 记录
* `java.lang.IllegalStateException: getWidth() and getHeight() must be overridden if background is null` 
  * TradeCategory 类中：JEI 要求当 getBackground() 返回 null 时，必须显式重写 getWidth() 与 getHeight() 方法返回 UI 的宽高
* 调用链路1
  ```txt
    Map<ResourceLocation, VillagerType> allVillagerType = MillenaireAPIHelper.getAllVillagerType();
    for (VillagerType vt : allVillagerType.values()){
        // 村民自己拥有的目标
        List<ResourceLocation> goals = vt.goals();
        // 如果目标是制作什么东西，建筑可能会抑制村民目标的运行
        // 这是多个建筑`json`文件的对象，对应村民Type所在的各个建筑
        List<BuildingPlanSet> bps = MillenaireAPIHelper.MappingVillagerBuilding(vt);
        // 这是一个建筑`json`文件的对象
        for (BuildingPlanSet bp : bps){
            // 这个映射是<a,变体1的声明>
            Map<String, List<BuildingPlanSet.LevelDef>> variants = bp.variants();
            // 这是具体变体的各个建筑等级
            for (List<BuildingPlanSet.LevelDef> lds : variants.values()){
                // 这是具体变体的一个建筑等级
                for (BuildingPlanSet.LevelDef bl : lds){
                    int l = bl.level(); // 这是等级
                    // 这个映射是"sand,16",指的是在这个等级下可以制作，库存16个(不展示库存，只展示可合成性)
                    // 被写入abstracted_production中会被阻碍？
                    // 不对不对，抽象合成好像是`离线火星收益`？
                    // 在`MarvelManager.java`中引用了`getAbstractedProduction`请求捐赠
                    Map<String, Integer> ap = bl.abstractedProduction();
                }
            }
        }
    }
  ```