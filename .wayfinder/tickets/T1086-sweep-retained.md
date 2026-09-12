---
id: T1086
title: sweepOrphans 保留计数读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
被 fork 引用保留的孤儿数不可见——加保留计数读数吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 43 轮 = effort #742 / spec 742 / impl 643）：DiskSpillStore 加 totalRetainedOrphans()（累计）+lastSweepRetained()（-1 哨兵=从未执行）——sweep 内计数入档，行为逐位不变只读升格。保留者强制关闭归 fork 生命周期机制。
