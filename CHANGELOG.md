# Millenaire JEI (millenairejeim) 更新日志

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
