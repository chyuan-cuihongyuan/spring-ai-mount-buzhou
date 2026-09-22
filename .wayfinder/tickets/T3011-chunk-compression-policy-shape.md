---
id: T3011
title: 分块压缩策略的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

冷块压缩判定与收益/代价怎么算？（spec 1905 / effort #1905 / R106）

## Resolution`

**TimescaleDB chunk 压缩策略纯计算 `ChunkCompressionPolicy`
（core/cleanup）**：shouldCompress（年龄 ≥ 阈值边界含上）+
savingsEstimate（original×(1−1/ratio)）+ readPenaltyFactor（读放大
代价 = ratio）。ratio > 1 fail-fast（≤1 压缩无意义）。落轮 grep
复核快速重传族占坑换静脉。
