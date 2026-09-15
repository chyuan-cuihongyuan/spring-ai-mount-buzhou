---
id: T1685
title: 预算钳位读面（BudgetClampStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1671
created: 2026-09-15
---

## Question

J 会话第 113 轮：budget 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：DefaultBudgetCalculator.evaluate 的 `Math.max(effective - fixedOverhead, 0)` 钳位——**负预算被钳 0 的发生频次**零计数（过大的固定开销吃穿上下文窗口时可用预算归 0，压缩判断失真信号）。纯预算域。

形状裁决：`DefaultBudgetCalculator` 内静态 `AtomicLong` 三计数——evaluations（入口）/ negativeClamps（钳位发生）/ normalBudgets（正常正预算）；嵌套 `record BudgetClampStats` + `stats()` + `resetForTest()`。守恒 evaluations = negativeClamps + normalBudgets。静态面理由同族先例；evaluate 返回语义逐位不变。

Out of scope：阈值判定分布（needed 已在 BudgetReport）；schemaTokensCache 命中面（缓存效率另轴）。
