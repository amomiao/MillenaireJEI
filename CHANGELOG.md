# Millenaire JEI (millenairejeim) 更新日志

---

## [v1.3.7] - 2026-11-11

### 🏷️ 运行时建筑标签图谱与 `buildingTag` / `requiredTag` 工艺解析修复 (ISSUE-005)
- **全量建筑标签与子建筑预索引 (`MillenaireDatabase` / `MillenaireAPIHelper`)**：
  - 在 `MillenaireDatabase` 中预构建建筑完整运行时标签索引（整合顶层 `BuildingPlanSet.tags()`、全变体/全等级 `ModCultures.getBuildingPlan(planId).tags()` 及子建筑 `LevelDef.parentTags()` 授予标签）与父子建筑结构映射；
  - 新增 `MillenaireAPIHelper.getBuildingAllTags`、`buildingHasTag`、`getBuildingsByTag(buildingTag, requiredTag, cultureFilter)` 与 `getBuildingSubBuildings`，将 `GoalSpotRecipe.resolveBuildingsWithTag` 与 `MillenaireAnomalyAnalyzer` 统一切换至全量标签判定。
- **遵循 Millénaire 9.1.0 `AbstractGatheringHandler.resolveCandidateBuildings` 语义**：
  - 扩展 `IMillCraftingRecipe`（默认方法 `getBuildingTag()` / `getRequiredTag()`）与 `MillCraftingBaseRecipe`（`setTagConstraints`、`applyTagConstraints`），并在 `MillCraftingRecipe.parse(...)` 统一注入 `handlerParams.buildingTag` 与 `handlerParams.requiredTag`；
  - 修复 `craft_iron_workshop`（`buildingTag="centralworkshop"` → 精准解析 `byzantines/workshop`）、`craft_mineiron`（`buildingTag="ironmine"` → `seljuk/lonemine`, `seljuk/mine_brick`）与 `craft_iron`（`requiredTag="forge"` → 剔除 `banditcamp` / `bandittower`）等配方的所属建筑解析错误，`GoalSpotRecipe` 同步对齐标签解析与确定性排序；
  - 在 `MillCraftingRecipeMaker#buildGoalToVillagerMap` 与 `GoalConfigRecipe#getAssociatedBuildingIds` 中补全确定性排序，覆盖 `GoalConfigRecipe#getBuildingTag()` 导出支持。

---

## [v1.3.6] - 2026-11-11

### ⚙️ 外部配置系统、独立配置目标页签与双写日志
- **MMLog 双写独立日志 (`logs/millenairejeim.log`)**：改造 `MMLog`，在保留 Minecraft 标准主日志（SLF4J）基础上同步双写 UTF-8 独立日志文件（每次客户端启动刷新独立会话横幅，提供 `getLogFilePath()` 与 `error(...)`），方便快速排查配置解析、未注入 Goal 告警与反射降级信息。
- **SpotGatheringGoal 外部配置化 (`config/millenairejeim/spot_gathering/`)**：
  - 彻底取消 `GoalSpotRecipe` 原有的硬编码 `switch (goalKey)`，首次启动由 `MillJeiConfigManager` 自动生成 `_README.txt` 与 6 份默认 JSON（`gather_silk`、`gather_snails`、`gather_brick`、`dry_brick`、`harvest_sugar_cane`、`plant_sugar_cane`）；
  - 当运行时自动化检测到未被配置覆盖的新 `SpotGatheringGoal` 时，去重输出双写 WARNING 告警并提示对应 `.json` 注入方式，同时在允许时按反射 `stockItem` 降级显示。
- **纯外部配置 Goal 独立页签 (`millenaire:custom_goals` / `config/millenairejeim/custom_goals/`)**：
  - 注册独立 JEI 页签 **`millenaire:custom_goals`**（**千年配置目标**），采用 **千年旅行书**（`ModItems.TRAVEL_BOOK`）作为图标，新建 `MillCustomGoalCategory`（继承 `BaseGoalCraftingCategory` 保持 `MillenaireJEIe` 加号联动兼容）、`GoalConfigRecipe`（支持 `goalIds` 合并同类目标并自动关联执行村民与建筑）与 `ConfigGoalLayoutView`；
  - 经完整审计 `millenaire-9.1.0.jar` 剩余 Goal 后预置 **25 份**高价值配置（`planting`、`paddy_planting`、`cocoa_planting`、`sapling_planting`、`flower_planting`、`take_from_building`、`gather_goods`、`get_tool`、`light_hearth`、`build_path`），默认排除纯 AI 作息、战斗与纯视觉手持物目标，且与 `/milljei export anomalies` 隔离不产生误报。
- **配置热重载与本地化**：新增客户端命令 `/milljei reload` 与 `/milljeim reload` 实时刷新外部配置缓存；补全 9 国语言包全部新增与历史缺漏键至统一 127 条；并将双写日志与配置扩展 Skill 沉淀至 `readme.md` 及 `.vibe/AI_Skill/`。

---

## [v1.3.5] - 2026-11-11

### 🏷️ 独立页签拆分：千年定点采集 (`MillSpotGatheringCategory`)
- **独立反射型定点工艺页签**：将依赖反射读取 `SpotGatheringGoal` 私有字段获取产物与限制信息的定点采集/处理工艺（`GoalSpotRecipe`：蚕茧、蜗牛紫染料、泥砖收集、湿砖日光晒干、甘蔗收种等）从【千年村庄工艺】拆出，独立注册为 **`millenaire:spot_gathering`**（**千年定点采集**）页签，并使用 **蚕块**（`ModItems.SILK_WORM_ITEM` / `millenaire:silk_worm`）作为页签图标。
- **泛型基类复用 (`BaseGoalCraftingCategory`)**：从 `MillCraftingCategory` 抽取通用工艺页签基类 `BaseGoalCraftingCategory<T extends IMillCraftingRecipe>`，派生 `MillCraftingCategory` 与强类型 `MillSpotGatheringCategory`，统一保留 `160x110` 布局、村民/建筑轮播槽位及与 `MillenaireJEIe` 联动的详情 `+` 跳转按钮，并在悬停类型/建筑行时展示 `buildingTag` 与 `townhallLimit` Tooltip。
- **JEI 动态路由与命令兼容**：`MillCraftingRecipeMaker` 拆分 `initStandardCraftingRecipes` 与 `getSpotGatheringRecipes`（`initRecipes` 保持返回全量供命令诊断使用）；`MillCraftingRecipeManagerPlugin` 新增 `GoalRegistry` 引用新鲜度检测，按 `CRAFTING_TYPE` 与 `SPOT_GATHERING_TYPE` 互斥分流缓存与 `IFocus` 焦点匹配。

---

## [v1.3.4] - 2026-11-11

### 🧪 工艺页签运行时硬编码与 Handler 补全
- **现有收割/捕鱼 Handler 补全**：
  - `GoalHarvestRecipe` 新增 `harvestItems` 多项/多概率掉落数组解析（自动跳过 `chance <= 0` 的除草清理 Goal 防止误报，合并同种作物产出的多轮骰概率提示并展示在 Tooltip 中）；补全 `paddy_harvesting`（`millenaire:rice_paddy` 输入）与 `cocoa_harvesting`（`minecraft:cocoa_beans x2` 产出与 `+1` 灌溉奖励）解析。
  - `GoalLootRecipe` 补全 `fishing_inuit` 无显式 `loot` 时的 `minecraft:cod` 保底产出与 `25%` 概率 `minecraft:bone_block` 附带 Tooltip。
  - 新增 `GoalChoppingRecipe` 支持 `chopping`（伐木与采叶），输入 `#minecraft:axes`（带不消耗工具提示），输出在 JEI 中按原木（8 种）与树苗/红树胎生苗（8 种，含每叶 `25%` 最多 3 叶提示）分组轮播并支持全部原木与树苗的 `R` 聚焦检索。
- **SpotGatheringGoal 运行时接入 (`GoalSpotRecipe`)**：
  - 自动检索激活注册表中的 `SpotGatheringGoal`，支持蚕茧（`gather_silk`）、蜗牛紫染料（`gather_snails`）、泥砖收集（`gather_brick`）、湿砖日光晒干（`dry_brick`）、甘蔗收割与种植（`harvest_sugar_cane` / `plant_sugar_cane`），并结合建筑标签与执行村民映射关联建筑。
- **InuitHuntingDropHandler 狩猎掉落接入**：
  - 新增鱿鱼（10% `1x seafood_raw`）、守卫者（`2-3x seafood_raw`）、远古守卫者（`5-9x seafood_raw`）、狼（`0-2x wolfmeat_raw`）与北极熊（`1-2x bearmeat_raw`）的 JEI 展示及触发条件（玩家击杀+知识 / 任意死亡）与掉落区间 Tooltip，同步支持 `CATALYST` 刷怪蛋焦点检索。
- **视图排版升级 (`HarvestLootLayoutView` / `EntityInteractionLayoutView` / `MillCraftingCategory`)**：
  - 支持超过 2 个产物的多格网格排布（最多 6 格及溢出轮播）与自定义条件摘要渲染。

### 🛡️ 安全反射防护 (`SafeReflectionHelper`)
- 新增双模组共用安全反射工具 `SafeReflectionHelper`（捕获全量 `Throwable` 并按 `类名#成员` 去重输出 `MMLog.warn` 三语告警，同时返回安全降级默认值），并改造 `MillenaireAnomalyAnalyzer` 使用该工具消除静默吞异常风险。

---

## [v1.3.3] - 2026-11-11

### 📦 依赖与工程维护
- **前置编译依赖升级**：将 `build.gradle` 中编译期前置依赖由 CurseMaven 切换为本地 `libs/millenaire-9.1.0.jar`（保持 `neoforge.mods.toml` 中的适用版本范围不变）。

### 🔍 异常诊断命令增强 (`/milljei export anomalies`)
- **无源输入检查 (`UNREACHABLE_INPUT`)**：按文化维度及全局维度构建生产源索引（整合 `GatheringGoal`、隐式动物/捕鱼/砍树掉落、Java 硬编码 `SpotGatheringGoal` 及 `foreignMerchantStock` 行商库存），自动检出缺乏上游产出的合成/熔炼输入（如拜占庭 `cook_iron_ore` 所需的 `minecraft:raw_iron`）。
- **产物语义不匹配检查 (`SEMANTIC_MISMATCH`)**：检出原料/`GoalKey`/`buildingLimit` 与最终产物存在材质或同族物品冲突的异常配方（如 `craft_clothes_silk` 消耗蚕丝却产出羊毛服饰 `clothes_byz_wool`）。

---

## [v1.3.2] - 2026-11-10

### 🛡️ 附属联动控件存在性治理
- **附属缺失时自动隐藏加号控件**：重构工艺页签（`MillCraftingCategory`）与交易页签（`MillTradeCategory`）中预留给附属的加号（`+`）按钮判断逻辑，将 `postRecipeButtonRender` 默认请求状态严格置为 `false`。
- **杜绝幽灵控件**：当扩展子模组（`millenairejeimm`）未安装或不存在时，加号控件与交互监听 100% 绝不加载与渲染，仅当附属模组存在且明确准许时才会展示。

---

## [v1.3.1] - 2026-11-10

### 🎨 界面与交互优化
- **交易页联动按钮重构**：将建筑详情查看加号按钮从顶行移至页面右下角（`X=142, Y=45`），彻底消除了与右上角 `[次要]` 交易标签的重叠冲突与视线遮挡。
- **输入安全保护**：在加号按钮与输入区域监听中加入 `!input.isSimulate()` 保护，避免 JEI 输入模拟测试阶段提前误触发。

### 📦 维护与生态
- 确立与联动模组 `MillenaireJEIe` 版本号同步机制，锁定后续升级仅更新小版本号（`1.3.x`）。

---

## [v1.3.0] - 2026-11-10

### 🚀 架构现代化与跨版本抽象
- **JEI 现代 API 迁移**：全面采用现代 JEI 标准契约（继承 `AbstractRecipeCategory`，移除废弃旧 API，使用 `IRecipeLayoutBuilder` 与 `IRecipeSlotsView` 规范构建）。
- **单分类多态聚合**：引入 `ICraftingLayoutView` 策略模式，统一聚合原木采伐、农作物收获、铁匠制造等多样化工艺，界面布局更协调。
- **跨版本抽象门面**：提取 `IPlatformBridge` 平台门面接口，为迁移适配 Minecraft 1.20.1 与 26.3 奠定架构基础。

### 🐛 问题修复
- **交易建筑名称本地化**：修复交易页签中“文明-建筑”名称未走语言文件翻译的问题（如 `日本-Tansetsu` 正确显示为本地化名称 `军械库/军匠铺`）。

### 🛠️ 诊断命令
- **多语言日志导出**：支持通过多语言日志器 `MMLog` 导出无村民/无建筑异常配方，以及全英文未本地化工艺列表，辅助排查翻译缺失。

---

## [v1.0.0 ~ v1.2.0]
- 提供千年村庄基础工艺与交易配方的 JEI 查询与展示支持。
