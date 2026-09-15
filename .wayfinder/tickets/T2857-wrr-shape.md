---
id: T2857
title: 平滑加权轮询序列的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

权重派发的比例与平滑怎么两全？（spec 1828 / effort #1828 / R29）

## Resolution

**NGINX smooth WRR 思想纯生成 `SmoothWeightedSequence`（core/exec）**：
sequence(weights, picks) 逐字保留 NGINX 算法（current+=weight → 峰值派出 →
−=总权重）；counts 直方；零权重不参与；确定性可回放；负 picks/空/负权/
全零 fail-fast。

