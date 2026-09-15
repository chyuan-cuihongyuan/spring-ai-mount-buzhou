---
id: T2899
title: 水库采样的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

流长未知的均匀采样怎么做？（spec 1849 / effort #1849 / R50）

## Resolution`

**Knuth 水库算法 R 思想纯函数 `ReservoirSample`（core/observability）**：
sample(k, seed, stream) 前 k 入池、第 i 个以 k/i 概率替换随机一员——终选
概率恰 k/n 与到达序无关；种子化 LCG 跨 JVM 重现（同种子同样本可回放，
EvalOrderRotator 同口径）；n≤k 全量保序、k=0 空。诊断采样从「攒全量再
随机」与「取前 N 到达序偏差」二选一变第三条路。

