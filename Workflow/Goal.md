# Human Claw Runtime
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