# Human Claw Runtime
## Workflow
* Q1:根据对HandlerParams的使用，哪些对象应该被归为一组? [附件](GatheringHandlerParamsLootReport.md)
  1. 显式战利品列表组（loot 驱动）归类对象：mining、fishing、fishing_inuit。  
     * 核心特征：显式配置 loot 数组，直接通过 item 与 count 声明产出物品和数量
  2. 方块成熟与采摘组（方块状态驱动）归类对象：harvesting、fruit_harvesting、cocoa_harvesting。  
     * 核心特征：围绕 targetBlock 进行目标判定；结合成熟度与年龄（如 targetState.age 或 ripeAge/resetAge）；产出依赖方块自掉落或显式的 harvestItem+harvestCount，部分作物支持 irrigationBonusCrop 灌溉奖励。
  3. 动物与生物行为组（实体参数驱动）归类对象：slaughter、breeding、shearing。  
     * 核心特征：依赖 animalType、buildingTag 或 requiredTag 等实体及区域属性；不使用通用 loot 结构，行为由生物机制控制（如屠宰使用 bonusItems 概率掉落、繁殖使用 foodItems）。
  4. ~~区域绑定与环境交互组（世界规则驱动）归类对象：chopping。~~(提示砍树给木头没意义)
     * 核心特征：仅声明 buildingTag 用于绑定工作区域；完全不配置任何物品掉落参数，产出由世界方块被破坏时的原生掉落逻辑决定。
* 给 4个`MillCrafting`、3个`type.base`以及`GoalCraftingRecipe`作为案例,尝试生成的一个新类型`Recipe`,还需要什么文件请告知后再进行;
* Q2:尝试生成的一个新类型`GoalLootRecipe`,还需要什么文件请告知后再进行;
  * 在开始为您生成 GoalLootRecipe.java 之前，请告知您希望采用哪种 JEI 显示架构，您偏向采用 选项 A（单页签） 还是 选项 B（独立页签）？
* Q3:不用改MillenaireJeiPlugin，和GoalCraftingRecipe渲染在同一个页签下
* Q4:/** 3.各种收割行为 */ public static final Set<String> HarvestTypeSet = Set.of("harvesting","fruit_harvesting","cocoa_harvesting"); 新增了字段，生成对应的Recipe，可以多留下一些内容方便以后拓展
* Q5:/** 4.与实体的各种行为 */ public static final Set<String> OfEntityTypeSet =Set.of("slaughter","breeding","shearing"); 新增了字段，生成对应的Recipe，可以多留下一些内容方便以后拓展

## 总之先理一下Goal
* Goal类型:`org.millenaire.goal.gathering.GatheringHandlerRegistry`
  * 所有继承了`org.millenaire.goal.gathering.handler.AbstractGatheringHandler`的内容在这里进行了注册,
    * 有19种工艺(共405份文件,其中合成288份,剩余117份),他们是`json`中"handler"字段的值:
      * breeding(繁殖) 前缀(4)`breed_`
      * chopping(砍伐) 唯一(1)`chop_trees`
      * smelting(熔炼) 前缀(18+1)`cook_`
        * 异常(1)`relight.json`也是`smelting`
      * crafting(合成) 前缀(287)`craft_`
      * take_from_building(从建筑取物) 拜占庭(3)`fetch_bread_`
      * fishing(钓鱼) 唯一(1)`fish`
      * fishing_inuit(因纽特式钓鱼) 唯一(1)`fishinuit`
      * fruit_harvesting(采摘水果) 前缀(6-1)`gather_` 
        * 异常(1)`gather_from_brick_kiln.json`是`take_from_building`
      * harvesting(收获) 前缀(21+3+4-1)`harvest_`
        * 异常(3)`remove_flower_home_`也是`harvesting`
        * 异常(4)`steal_tulip_`也是`harvesting`
        * 异常(1)`harvest_cocoa`是`cocoa_harvesting`
      * mining(采矿) 前缀(15)`mine_`
      * flower_planting(种植花卉) 拜占庭(3)`gardening_`
      * planting(种植) 前缀(23-5-1)`plant_`
        * 异常(5) `plant_*sapling` 是 `sapling_planting`
        * 异常(1)`plant_cocoa`是`cocoa_planting`
      * shearing(剪毛) 唯一(1)`shear_sheep`
      * slaughter(屠宰) 前缀(12)`slaughter_`
      * sapling_planting(种植树苗) -> planting
      * cocoa_planting(可可种植) -> planting
      * cocoa_harvesting(可可收获) -> harvesting
      * ~~paddy_planting(水稻种植)~~
        * `plant_wheat_paddy`是`plant`
      * ~~paddy_harvesting(水稻收割)~~
        * `harvest_wheat_paddy`是`harvesting`
  * 总的来说分为四种类似
    * 配方类[依赖json]: `crafting`和`smelting`
      * 有`inputs`(支持物品ID或 Tag 标签)和`outputs`
    * 战利品生成类[依赖战利品表]:
      * 世界交互战利品: `chopping`、`mining`
      * 钓鱼战利品: `fishing`、`fishing_inuit`
      * 收获战利品: `harvesting`、`fruit_harvesting`、`cocoa_harvesting`、`shearing`、`slaughter`、`breeding`
        * 有`村庄灌溉度(irrigationBonus)`存在额外产量
    * 农耕种植: `planting`、`sapling_planting`、`cocoa_planting`、`flower_planting`
      * 种植：扫描建筑的 soilPositions（土壤点），消耗种子进行摆放； 
      * 收割：检测方块状态（如 COCOA 的 AGE 熟化度），破坏成熟作物，并结合村庄灌溉度（irrigationBonus）计算额外产量。
    * 其他: 
      * `take_from_building` 拜占庭的物流系统
* 看来`丝绸`就是没有声明遗漏了
* 加载Goal:`org.millenaire.goal.gathering.GatheringTypeLoader`
  * 但是部分很多Goal没有显式的文件声明