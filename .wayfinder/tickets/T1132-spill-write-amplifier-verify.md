---
id: T1132
title: Spill 写放大读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1131]
created: 2026-09-13
---

## Question

放大率/近窗滑动/P95 混排如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 16 轮 = effort #815）：SpillWriteAmplifierTest 5 例——store 1200+markLinked 1400 → 放大率 1.3 精确/近窗 3.0 vs 总均值被稀释/零负三形态忽略/空真/混排 P95=5.0+近窗均值 1.3125（窗口账修正后绿）。
