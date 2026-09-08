# handlerParams 战利品与收获调查回执

调查范围：405 份 JSON 中与世界交互战利品、钓鱼战利品、收获战利品相关的 10 个 handler：

- 世界交互：`chopping`、`mining`
- 钓鱼：`fishing`、`fishing_inuit`
- 收获/产出：`harvesting`、`fruit_harvesting`、`cocoa_harvesting`、`shearing`、`slaughter`、`breeding`

本次不调查农耕种植：`planting`、`sapling_planting`、`cocoa_planting`、`flower_planting`。

统计按 JSON 顶层 `handler` 的实际值进行。字段“出现数量”是该 handler 下实际包含该字段的文件数，不代表运行时调用次数。

## 1. 抽样文件与 handlerParams 形状

| handler | 文件数量 | 抽样文件 | handlerParams 形状 |
|---|---:|---|---|
| `chopping` | 1 | `chop_trees.json` | `buildingTag`；不声明 `loot`，砍伐产出由世界方块交互决定 |
| `mining` | 15 | `mine_clay.json`, `mine_clay_byz.json` | `sourceSubtype`、`loot`；部分文件有 `buildingTag` |
| `fishing` | 1 | `fish.json` | `loot` 数组 |
| `fishing_inuit` | 1 | `fishinuit.json` | 与 `fishing` 相同，使用 `loot` 数组 |
| `harvesting` | 26 | `harvest_carrot.json`, `harvest_flower_poppy.json` | 作物类使用目标方块/目标状态；花卉类使用 `harvestItem`/`harvestCount` |
| `fruit_harvesting` | 5 | `gather_cider_apples_home.json`, `gather_olives.json` | 完整的方块成熟度收获模型 |
| `cocoa_harvesting` | 1 | `harvest_cocoa.json` | 当前仅有 `soilSubtype: "cacao"` |
| `shearing` | 1 | `shear_sheep.json` | 仅有 `buildingTag: "sheeps"`；羊毛产出由剪毛行为决定 |
| `slaughter` | 12 | `slaughter_chicken.json`, `slaughter_cow_inuit.json` | `animalType`、`buildingTag`、`requiredTag`、`damage`；部分文件额外有 `bonusItems` |
| `breeding` | 4 | `breed_cattle.json`, `breed_chicken.json` | `animalType`、`buildingTag`、`foodItems`；繁殖不是物品 loot 模型 |

## 2. 五个重点字段的调用情况

### 2.1 `targetBlock`

| handler | 出现情况 | 作用 |
|---|---|---|
| `harvesting` | 26/26 | 指定要检查/收获的作物、花、藤蔓或郁金香方块 |
| `fruit_harvesting` | 5/5 | 指定果树叶方块，如 `millenaire:apple_tree_leaves` |
| 其他 8 类 | 0 | 当前样本和全量文件均未使用 |

`targetBlock` 是“方块目标”，不是产物。它需要与 `targetState` 或成熟度字段配合，决定方块是否达到可收获状态。

### 2.2 `ripeAge`

| handler | 出现情况 | 作用 |
|---|---|---|
| `fruit_harvesting` | 5/5 | 与 `ageProperty` 配合，表示达到该年龄值后才采摘 |
| 其他 9 类 | 0 | 未出现 |

果树样本：

```json
{
  "targetBlock": "millenaire:apple_tree_leaves",
  "ageProperty": "age",
  "ripeAge": 3,
  "resetAge": 0,
  "harvestItem": "millenaire:cider_apple",
  "harvestCount": 1
}
```

`ripeAge` 不是通用作物成熟字段。普通 `harvesting` 使用的是 `targetState.age`，例如：

```json
{
  "targetBlock": "minecraft:carrots",
  "targetState": { "age": 7 }
}
```

### 2.3 `harvestItem`

| handler | 出现情况 | 作用 |
|---|---|---|
| `fruit_harvesting` | 5/5 | 指定果树采摘后生成的物品 |
| `harvesting` | 7/26 | 仅花卉收获子集使用 |
| 其他 8 类 | 0 | 未出现 |

`harvesting` 中出现该字段的文件是：

`harvest_flower_blue_orchid.json`、`harvest_flower_dandelion.json`、`harvest_flower_home_blue_orchid.json`、`harvest_flower_home_dandelion.json`、`harvest_flower_home_poppy.json`、`harvest_flower_home_rosebush.json`、`harvest_flower_poppy.json`。

普通作物收获通常不声明 `harvestItem`，产出由作物方块自身的掉落规则决定。

### 2.4 `harvestCount`

| handler | 出现情况 | 作用 |
|---|---|---|
| `fruit_harvesting` | 5/5 | 与 `harvestItem` 成对使用，指定每次采摘数量 |
| `harvesting` | 7/26 | 与花卉 `harvestItem` 成对使用 |
| 其他 8 类 | 0 | 未出现 |

因此，`harvestCount` 不是所有收获行为的统一产量字段，而是显式指定采摘物时使用的数量。

### 2.5 `buildingTag`

| handler | 出现情况 | 典型值 | 调查解释 |
|---|---:|---|---|
| `chopping` | 1/1 | `grove` | 将砍伐任务绑定到指定建筑/区域标签 |
| `mining` | 9/15 | `sandpit`、其他建筑标签 | 将采矿任务绑定到建筑或资源场所；没有该字段的矿物任务仍可执行世界采矿 |
| `fishing` | 0/1 | 无 | 不需要建筑标签 |
| `fishing_inuit` | 0/1 | 无 | 不需要建筑标签 |
| `harvesting` | 15/26 | `carrot`、`garden` 等 | 指定作物/花卉对应的建筑或区域标签；home 变体可能省略 |
| `fruit_harvesting` | 4/5 | `childrenorchard` 等 | 指定果园或果树区域；home 变体可省略 |
| `cocoa_harvesting` | 0/1 | 无 | 当前文件只有 `soilSubtype` |
| `shearing` | 1/1 | `sheeps` | 指定羊圈或相关建筑标签 |
| `slaughter` | 12/12 | `chicken`、`cattle` 等 | 指定屠宰建筑/动物区域标签 |
| `breeding` | 4/4 | `cattle`、`chicken` 等 | 指定繁殖建筑/动物区域标签 |

`buildingTag` 不是战利品本身，也不是产物 ID。它是任务寻找、限制或绑定建筑/区域的语义标签；同一个字段在不同 handler 中绑定的建筑类型不同。

## 3. 战利品字段的实际分工

### 3.1 `loot`：显式物品战利品

当前只出现在：

- `mining`：15/15。每个文件都声明至少一个 `item` + `count`，例如矿石、黏土球、砂石等。
- `fishing`：1/1。`fish.json` 返回鳕鱼。
- `fishing_inuit`：1/1。`fishinuit.json` 同样返回鳕鱼。

`chopping` 没有 `loot`。砍伐产出由砍伐 handler 对世界方块的交互和方块掉落决定。

C# 对应载体是 `List<GatheringRecipeEntry>? Loot`，条目字段为 `item` 和 `count`。

### 3.2 收获类：目标状态与显式产物并存

`harvesting` 有两套模式：

1. 作物/藤蔓模式：`targetBlock` + `targetState`，部分文件附带 `irrigationBonusCrop`；不直接声明 `harvestItem`。
2. 花卉模式：`targetBlock` + `harvestItem` + `harvestCount`，部分文件还带 `buildingTag`。

`fruit_harvesting` 是另一套成熟度模型：

`targetBlock` + `ageProperty` + `ripeAge` + `resetAge` + `harvestItem` + `harvestCount`。

`cocoa_harvesting` 当前不使用上述显式成熟度/产物字段，仅通过 `soilSubtype` 标识 cacao 类型；不可根据 `harvesting` 或 `fruit_harvesting` 的模型推断可可的参数调用。

### 3.3 生物行为：产出不通过 `loot`

- `shearing`：只声明 `buildingTag`；剪毛产出由 shearing handler / 生物行为决定。
- `slaughter`：通过 `animalType`、`requiredTag`、`damage` 执行屠宰，部分因纽特/诺曼/塞尔柱样本有 `bonusItems`。
- `breeding`：通过 `animalType`、`foodItems` 和 `buildingTag` 执行繁殖，不声明 `loot`。

`bonusItems` 是屠宰的额外掉落，不等同于通用 `loot`：

```json
{
  "item": "minecraft:bone",
  "chance": 50
}
```

C# 对应载体是 `List<GatheringBonusItemEntry>? BonusItems`，成员为 `item` 和 `chance`。

## 4. irrigationBonus 的核查

目录中的实际字段名是 `irrigationBonusCrop`，不是 `irrigationBonus`。在本次调查的 10 个 handler 中，它只出现在 `harvesting` 的 12 个作物类文件：

`harvest_carrot.json`、`harvest_carrot_home.json`、`harvest_cotton.json`、`harvest_cotton_home.json`、`harvest_maize.json`、`harvest_potato.json`、`harvest_potato_home.json`、`harvest_turmeric.json`、`harvest_vines.json`、`harvest_wheat.json`、`harvest_wheat_home.json`、`harvest_wheat_paddy.json`。

典型形式：

```json
{
  "targetBlock": "minecraft:carrots",
  "targetState": { "age": 7 },
  "soilSubtype": "carrot",
  "buildingTag": "carrot",
  "irrigationBonusCrop": "minecraft:carrot"
}
```

它的语义是：当村庄灌溉度满足条件时，以 `irrigationBonusCrop` 指定额外产物/作物，而不是修改 `targetBlock` 的目标或替代 `harvestItem`。本次不调查种植 handler，因此没有把种植侧字段混入结论。

## 5. 对 C# 超集类的对应关系

现有 `GatheringHandlerParameters` 已覆盖本次调查到的成员：

- `targetBlock` -> `TargetBlock`
- `targetState.age` -> `TargetState.Age`
- `ageProperty` -> `AgeProperty`
- `ripeAge` -> `RipeAge`
- `resetAge` -> `ResetAge`
- `harvestItem` -> `HarvestItem`
- `harvestCount` -> `HarvestCount`
- `buildingTag` -> `BuildingTag`
- `irrigationBonusCrop` -> `IrrigationBonusCrop`
- `loot` -> `Loot`
- `bonusItems` -> `BonusItems`

结论：这些成员应继续作为超集类的可空成员，由具体 handler 按自身语义读取；不能把 `ripeAge`、`harvestItem`、`harvestCount` 或 `loot` 标为所有收获/战利品 handler 的必需字段。
