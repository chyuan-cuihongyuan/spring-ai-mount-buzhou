---
id: T2813
title: 检查点滞后读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

崩溃重放成本与检查点节奏健康度怎么量化？（spec 1806 / effort #1806 / R7）

## Resolution

**Kafka consumer lag / SQLite WAL checkpoint 思想纯读面
`CheckpointLagReadout`（core/recovery）**：SessionLag(sessionId, produced,
checkpointed) 单会话事实（构造器核 id 非空+0≤checkpointed≤produced 契约，
lag()=差值）；analyze → LagReport（totalLag 全会话重放成本 / maxLag 最坏
单会话 -1 哨兵 / laggiestUser 并列取首）+ sessionsBeyond(threshold) 越限
计数 + caughtUpRatio 追平率（无会话 -1 哨兵）。

