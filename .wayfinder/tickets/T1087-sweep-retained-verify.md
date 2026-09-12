---
id: T1087
title: sweepOrphans 保留计数读数验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1086]
created: 2026-09-13
---

## Question
保留与清除分离可见如何证明？

## Resolution
**用户常设授权 AFK（可推翻）**

验证（G 会话第 43 轮 = effort #742）：fork 引用保留计入 totalRetainedOrphans/lastSweepRetained=1+无引用清除 deleted=1+存在性断言+初始 -1 哨兵。buzhou-spill 全模块零回归（C 会话排除集）。
