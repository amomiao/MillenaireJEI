# MillenaireJEI 跨版本迁移语料库 (Minecraft 1.20.1 & 26.3)

详细技术文档见 Artifact:
[MillenaireJEI_Migration_Corpus_1.20.1_and_26.3.md](file:///C:/Users/hanji/.gemini/antigravity/brain/1b4e0734-0543-4f51-a29d-3841bb9916a4/MillenaireJEI_Migration_Corpus_1.20.1_and_26.3.md)

## 核心映射概览
1. 平台抽象层: `IPlatformBridge` 在 1.20.1 映射到 `Forge1201PlatformBridge`，在 26.3 映射到 `NeoForge263PlatformBridge`。
2. 事件总线: 1.20.1 使用 `MinecraftForge.EVENT_BUS`，1.21.1 与 26.3 使用 `NeoForge.EVENT_BUS`。
3. 物品组件: 1.20.1 使用 `CompoundTag` / `ItemStack.getTag()`，1.21.1+ 使用 `DataComponents`。
4. JEI 差异: 1.20.1 (JEI 15) 采用旧版 drawable 动画，1.21.1+ (JEI 19+) 采用 `createRecipeExtras` 声明式箭头。
5. 本地化规范: 建筑统一使用 `building.millenaire.<culture>.<building_id>`，切勿传 `nativeName`。
