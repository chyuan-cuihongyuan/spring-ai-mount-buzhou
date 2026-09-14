---
id: T1629
title: memory 域双工具组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1627
created: 2026-09-15
---

## Question

J 会话第 87 轮：memory 域双工具组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R59 CompactNowTool（手动压缩）与 R55 EpisodeLedger（情景记忆）双读面同域协同——独立性与一致性无验证。纯测试轮第五弹。

形状裁决：新增 `MemoryToolsReadoutTest`（buzhou-memory）——compact_now 调用与 EpisodeLedger record/recall 交叉后，双读面各自守恒保持、互不串账、reset 独立隔离。零生产改动。
