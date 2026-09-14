---
id: T2199
title: 轮次采样漏斗读面（TurnSamplerHook 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 49 轮：采样漏斗归因读面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：TurnSamplerHook 采样判定（哈希确定性+短问过滤+fail-soft）零漏斗读面——「为何没进数据集」无法归因。

形状裁决：静态漏斗增量——turnsSeen/emptySkipped/shortSkipped/rateSkipped/written/writeFailures 五桶+快照+reset；桶口径 turnsSeen=written+rate+short+empty（ratePercent=0 早退不入漏斗）；采样/fail-soft 语义逐位不变。

Out of scope：写入重试；会话分桶；数据集容量。
