---
id: T2913
title: 利特尔法则审计的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

跨指标互证怎么基建？（spec 1856 / effort #1856 / R57）

## Resolution`

**排队论 Little's Law（L=λ×W）思想纯审计 `LittlesLawAudit`
（core/metrics）**：impliedConcurrency(λ, W) 毫秒换算内置 +
consistency(measuredL, λ, W, tol) → CONSISTENT/DIVERGENT（|L−λW| ≤
tol×max(|λW|,1) 零基线退化）。稳态下三指标必然互证——偏差即仪表失真
或稳态破，互证比单指标自证可信一个量级。

