# MillenaireJEI 升级与跨版本抽象架构方案

详细设计与 Mermaid 架构图见 Artifact:
[MillenaireJEI_Upgrade_and_CrossVersion_Proposal.md](file:///C:/Users/hanji/.gemini/antigravity/brain/1b4e0734-0543-4f51-a29d-3841bb9916a4/MillenaireJEI_Upgrade_and_CrossVersion_Proposal.md)

## 核心要点
1. 前端规范对齐现代 JEI (AbstractRecipeCategory + IRecipeExtrasBuilder + ViewBasedCategory 策略模式)。
2. 对 MillenaireJEIe 保持 100% 源码与二进制兼容 (11 个核心类保持全限定名与方法签名不变，采用 Facade 门面模式)。
3. 跨版本分层抽象 (1.20.1 Forge / 1.21.1 NeoForge / 目标版本 26.3):
   - IPlatformHelper 隔离 NBT 与 DataComponent 差异
   - IEventBridge 隔离 Forge EventBus 与 NeoForge EventBus 差异
   - Category 骨架与 View 策略复用
