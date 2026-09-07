# Millénaire JEI Support (千年村庄 JEI 扩展)

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-blue.svg)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.0+-orange.svg)
![JEI Support](https://img.shields.io/badge/JEI-Supported-green.svg)
![Side](https://img.shields.io/badge/Side-Client--Only-brightgreen.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

本模组为 Minecraft 1.21.1（NeoForge）环境下的 **Millénaire（千年村庄）** 模组提供 **JEI (Just Enough Items)** 交易与配方检索支持，为**纯客户端（Client-Only）**扩展。

---

## 功能特性

* **村庄交易检索**
  * **买卖列表**：提供各文化建筑内商品出售价格（金币/银币/铜币）、声望门槛及收购需求的完整数据查询。
  * **数据汇总**：对分布在不同建筑内的相同交易进行合并展示，并支持悬浮窗查看详细建筑信息。
* **动态工艺合成**
  * **GoalRegistry集成**：对接 Millénaire 内部逻辑，支持按需加载村民（如铁匠、木匠等）制作的衍生物品及中间件配方。
* **方块染色查询**
  * **油漆染色**：支持检索油漆桶对千年村庄特有方块（花纹砖、楼梯、台阶、墙体等 16 色变体）的染色与重染色配方。

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

项目开发与配方对接工作持续优化中，如遇到配方未显示、本地化缺失或程序崩溃等问题，请在评论、项目的 **Issues** 页面提交详细日志与现象说明。

---

## 开源协议

本项目采用 [MIT License](LICENSE) 协议开源。