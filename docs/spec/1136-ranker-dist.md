# 1136 — SemanticSkillRanker 排序分布深化

> 来源：J 会话第 121 轮 = effort #1136（[T1701](../../.wayfinder/tickets/T1701-rankerdist-shape.md) / [T1702](../../.wayfinder/tickets/T1702-rankerdist-verify.md) / impl 875）。R74 同款单计数深化。

## Problem Statement

SemanticSkillRanker 仅 bypassCount 单计数——排序调用总量、跳过分布不可见。

## 目标

- 追加静态三计数 `rankCalls` / `skippedTrivial` / `succeeded`（bypassed 既有实例字段保持）。
- 嵌套 `record RankerDistStats(long rankCalls, long skippedTrivial, long bypassed)` + `distStats()` + `resetDistForTest()`。

## 兼容性

纯增量读面：rank 返回语义逐位不变；bypassCount 既有公共 API 保持。

## Out of Scope

- 相似度分数分布（展示面）。
