---
id: T2839
title: 花费匀速曲线的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

预算花得多快怎么判态？（spec 1819 / effort #1819 / R20）

## Resolution

**广告 spend pacing 思想纯判态 `BudgetPacingCurve`（core/budget）**：
evaluate(elapsed, spent, tolerance) 三态 ON_PACE/OVER_PACING/UNDER_PACING
（偏离=spent−elapsed，带内容差含边界）+ runRate() 运行率（spent/elapsed，
elapsed=0 -1 哨兵）。FLOAT_EPSILON=1e-12 边界浮点噪声免疫（实现侧根治
而非测试侧绕开）。

