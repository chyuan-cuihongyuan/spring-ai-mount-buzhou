---
id: T3021
title: 随机早期丢弃的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

早期概率丢弃的曲线怎么算？（spec 1910 / effort #1910 / R111）

## Resolution`

**RED 经典三段纯计算 `RandomEarlyDrop`（core/backpressure）**：
dropProbability（minTh 下 0/两线线性到 maxP/maxTh 上 maxP）+ zone
三区读数（BELOW/LINEAR/ABOVE 预警区）。minTh<maxTh/maxP∈(0,1]
fail-fast。落轮 grep 复核无占坑。
