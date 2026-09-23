---
id: T3049
title: 空闲连接收割的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

闲置连接的收割判定与排序怎么算？（spec 1924 / effort #1924 / R125）

## Resolution`

**HikariCP idle reaper 语义纯计算 `IdleConnectionReaper`
（core/concurrent）**：reapCandidates（闲置 ≥ maxIdle 入选，闲置
最久排前，边界含上）+ idleMillis 闲置时长读数。时点非负/now ≥
lastUsed/maxIdle ≥ 1 fail-fast。落轮 grep 复核无占坑。
