# Millénaire JEI Support (千年村庄 JEI 扩展)

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-blue.svg)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.0+-orange.svg)
![JEI Support](https://img.shields.io/badge/JEI-Supported-green.svg)
![Side](https://img.shields.io/badge/Side-Client--Only-brightgreen.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

本模组为 Minecraft 1.21.1（NeoForge）环境下的 **Millénaire（千年村庄）** 模组提供 **JEI (Just Enough Items)** 交易、工艺、定点采集、配置目标与油漆染色检索支持，为**纯客户端（Client-Only）**扩展。

---

## 功能特性

* **村庄交易检索 (`Village Selling` / `Village Buying`)**
  * **买卖列表**：提供各文化建筑内商品出售价格（金币/银币/铜币）、声望门槛及收购需求的完整数据查询。
  * **数据汇总**：对分布在不同建筑内的相同交易进行合并展示，并支持悬浮窗查看详细建筑信息。
* **动态工艺合成 (`Millénaire Crafting`)**
  * **GoalRegistry 集成**：自动接入 Millénaire 内部 `crafting`、`smelting`、`mining`、`fishing`/`fishing_inuit`、`harvesting`、`paddy_harvesting`、`cocoa_harvesting`、`chopping`、`slaughter`、`breeding`、`shearing` 以及 `inuit_hunting` 狩猎掉落体系，并带有 `SafeReflectionHelper` 安全反射降级保护。
* **千年定点采集 (`millenaire:spot_gathering`)**
  * **半自动外部注入（蚕块图标）**：彻底取消 Java 硬编码 `switch`，改为从 `config/millenairejeim/spot_gathering/*.json` 加载 `SpotGatheringGoal` 的输入、催化剂、产物与说明（预置蚕茧、蜗牛紫染料、砖块收集、湿砖晒干、甘蔗收割与种植 6 份模板）。
  * **新对象未注入检测**：若后续 Millénaire 或扩展包新增了尚未在 `spot_gathering/` 登记的 `SpotGatheringGoal`，模组会去重输出 WARNING 日志至主日志与独立日志，并按反射 `stockItem` 尽量降级显示。
* **千年配置目标 (`millenaire:custom_goals`)**
  * **纯外部配置页签（旅行书图标）**：100% 由 `config/millenairejeim/custom_goals/*.json` 驱动，预置 **25 份**经过审计筛除纯 AI 目标的高价值剩余 Goal（农作物/水稻/可可/树苗/花卉种植、跨建筑调拨、地面捡拾、工具升级、炉灶点火、道路铺设），支持 `goalIds` 自动关联执行村民与建筑，兼容 `MillenaireJEIe` 详情加号跳转。
* **方块染色查询 (`Paint Dyeing`)**
  * **油漆染色**：支持检索油漆桶对千年村庄特有方块（花纹砖、楼梯、台阶、墙体等 16 色变体）的染色与重染色配方。

---

## 外部配置目录 (`config/millenairejeim/`)

首次启动客户端会自动创建以下两个配置目录并生成 `_README.txt` 与默认 `.json` 模板（仅在目录尚不存在时初始化，之后完全保留你的自定义增删改；支持单对象 JSON 或 `{ "entries": [ ... ] }` 多条目数组）：

1. **`config/millenairejeim/spot_gathering/`**（半自动覆盖 `SpotGatheringGoal`）
   * 核心字段：`enabled`、`goalId`（如 `millenaire:dry_brick`）、`inputs`、`catalysts`、`outputs`、`useReflectedStockItemFallback`、`buildingTagOverride`、`townhallLimitOverride`、`extraInfoKey` / `extraInfoArgs` / `extraInfoText`、`extraTooltipKeys` / `extraTooltips`、`outputTooltipKeys` / `outputTooltips`。
   * 若需要隐藏某条 `SpotGatheringGoal` 且不再产生未注入警告，可创建对应 JSON 并设置 `"enabled": false`。
2. **`config/millenairejeim/custom_goals/`**（纯外部配置型 Goal 页签）
   * 核心字段：`enabled`、`id`、`goalKey`、`goalIds`（可绑定多个同类目标如 `_home` / `_paddy` 合并展示）、`handlerId`（对应本地化前缀 `jei.millenaire.crafting_type.<handlerId>`）、`titleKey` / `title`、`inputs`、`catalysts`、`outputs`、`extraInfoKey` / `extraInfoArgs` / `extraInfo`、`extraTooltipKeys` / `extraTooltips`、`villagerIds` / `villagerSummary`、`buildingIds` / `buildingTags` / `buildingSummary`、`autoBindGoalVillagers`、`autoBindBuildingsFromVillagers`、`sortOrder`。
   * 物品描述符通用写法：`{ "item": "minecraft:wheat_seeds", "count": 1 }` 或标签 `{ "tag": "minecraft:saplings", "count": 1 }`，可选附加 `"tooltipKeys"` 与 `"tooltips"`。

### 客户端命令
* `/milljei reload` 或 `/milljeim reload`：清理并重载 `config/millenairejeim/` 配置缓存（随后重开或重新查询 JEI 即可看到最新配置，无需重启游戏）。
* `/milljei export anomalies`：导出无源输入与语义不匹配异常报告（仅统计常规合成/定点采集链路，不会被纯配置型教学条目干扰）。

---

## 双写独立日志 (`logs/millenairejeim.log`)

* **双写输出**：`MMLog` 在写入 Minecraft 标准主日志（SLF4J）的同时，会自动向客户端游戏根目录下的 `logs/millenairejeim.log`（UTF-8）写入独立会话日志（每次启动覆盖开启新会话横幅）。
* **建议排查方式**：遇到 **未注入 `SpotGatheringGoal` 告警**、**配置 JSON 格式/物品 ID 无效警告** 或 **反射字段变化兼容提示** 时，优先查阅 `logs/millenairejeim.log` 即可快速定位全部 `Millenaire-JEI` 诊断信息。

---

## 相关 Skill 与扩展指南

面向开发者、整合包作者与 AI 协作代理的项目技能已沉淀至 [`.vibe/AI_Skill/MillenaireJEI_ConfigGoal扩展与双写日志.skill.md`](.vibe/AI_Skill/MillenaireJEI_ConfigGoal扩展与双写日志.skill.md)（登记于 [`.vibe/AI_Skill/index.md`](.vibe/AI_Skill/index.md)），核心要点如下：

* **Skill 名称**：`MillenaireJEI_ConfigGoal扩展与双写日志`
* **触发场景**：
  1. 排查 `logs/millenairejeim.log` 中的 `SpotGatheringGoal` 未注入告警与降级状态；
  2. 为 `Millénaire` 新版本或魔改整合包增删 `spot_gathering/*.json` / `custom_goals/*.json`；
  3. 需要按照既有架构延伸内置 `CustomGoal` 默认模板与 9 语种本地化同步。
* **剩余 Goal 收录准绳**：
  * **应收录入 `custom_goals/`**：存在具体物品消耗、种植/转化产出、特定建筑间物资转移、或关键工具/方块世界交互的目标（如 `planting` / `paddy_planting` / `cocoa_planting` / `sapling_planting` / `flower_planting` / `take_from_building` / `gather_goods` / `get_tool` / `light_hearth` / `build_path`）。
  * **默认排除不写入**：纯 AI 状态/作息（`idle`、`rest`、`sleep`、`socialise`、`pray*`、`patrol`）、战斗目标与纯视觉手持物展示（如 `VisitTask` 展示道具）。
* **热验证闭环**：编辑 `config/millenairejeim/*.json` -> 游戏内执行 `/milljei reload` -> 打开 JEI 验证槽位与悬浮提示 -> 检查 `logs/millenairejeim.log` 确认加载条数无报错。

---

## 运行端说明

* **仅客户端安装**：本模组仅作用于客户端，放入客户端 `mods/` 目录即可生效。
* **服务端兼容**：已配置 `displayTest="NONE"`，玩家可自由连接任何单人世界或专用服务端（Dedicated Server），服务端无需安装此模组。

---

## 前置需求

* **Minecraft**: `1.21.1`
* **Mod Loader**: `NeoForge`
* **前置模组**:
  * [Millénaire (千年村庄)](https://www.curseforge.com/minecraft/mc-mods/millenaire)
  * [JEI (Just Enough Items)](https://www.curseforge.com/minecraft/mc-mods/jei)

---

## 问题反馈

项目开发与配方对接工作持续优化中，如遇到配方未显示、本地化缺失或程序崩溃等问题，请在评论、项目的 **Issues** 页面提交 `logs/millenairejeim.log` 与现象说明。

---

## 开源协议

本项目采用 [MIT License](LICENSE) 协议开源。
