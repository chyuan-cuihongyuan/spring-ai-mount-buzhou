---
id: T2107
title: EWMA 自适应超时推荐器（AdaptiveTimeout）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 4 轮（换题轮）：超时维自适应推导器的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：原题 retry budget 与 RetryBudget/RetryBudgetHolder（spec 178 Finagle retry budget）全撞——顺延 EWMA 自适应超时（grep -i ewma/adaptiveTimeout 零命中）。

形状裁决：`AdaptiveTimeout` 纯推导器（resilience 根包）——EWMA（α=0.3 默认、CAS 无锁、首样本播种）+ 推荐 clamp(⌈EWMA×multiplier⌉, floor, ceiling)（3×、[50ms,60s] 默认）+ 预热哨兵（<3 样本 empty，BudgetRecommendation 先例）+ stats() 无副作用 + resetForTest()；不接线执行路径（超时执行归既有 turnBudget 族，装配留后续）。

Out of scope：分位推导；per-provider 编排；执行接线。
