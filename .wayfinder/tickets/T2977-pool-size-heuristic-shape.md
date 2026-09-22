---
id: T2977
title: 连接池容量启发的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

单节点池容量公式与多节点预算拆分怎么算？（spec 1888 / effort #1888 / R89）

## Resolution`

**HikariCP wiki 公式纯计算 `PoolSizeHeuristic`（core/concurrent）**：
optimalSize（cores×2+spindles）+ splitBudget（预算均衡拆分余数摊平）
+ saturationRatio（饱和度读数零池哨兵 0.0）。cores≥1/spindles≥0/
预算≥节点数 fail-fast。落轮 grep 复核无占坑。
