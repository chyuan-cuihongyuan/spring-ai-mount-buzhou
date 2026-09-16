---
id: T3155
title: EWMA 估计器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

指标观测的平滑层怎么原语化？（spec 2027 / effort #2027 / R28）

## Resolution

**Netflix/Finagle 口径 `EwmaEstimator`（core/metrics，单写者）**：
首样本直接锚定（非 0 爬坡）+estimate=α×新+(1−α)×旧（α∈(0,1] 默认
0.2≈5 样本记忆——1 直通最新、小 α 惯性大）+hasSamples/observations/
reset 读数——尖峰被稀释、趋势仍跟随。
