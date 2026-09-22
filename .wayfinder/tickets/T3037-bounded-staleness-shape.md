---
id: T3037
title: 有界旧读的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

读旧度的合同判定怎么算？（spec 1918 / effort #1918 / R119）

## Resolution`

**Cosmos DB bounded staleness 纯计算 `BoundedStaleness`
（core/transaction）**：stalenessMillis（read−lastWrite 钳 0——时钟
偏斜负旧度无意义）+ verdict 两态（≤ bound WITHIN_BOUND 边界含上/
> STALE）。时点/界非负 fail-fast。落轮 grep 复核无占坑。
