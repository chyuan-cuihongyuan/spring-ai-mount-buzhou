---
id: T3031
title: 百分位排位的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

样本内相对排位怎么算？（spec 1915 / effort #1915 / R116）

## Resolution`

**统计学 percentile rank 纯计算 `PercentileRank`（core/eval）**：
rank（≤ value 占比，越界钳 0.0/1.0 不捏造中间值）+ percentileOf
（×100 直读报表口径）。samples 非空非负 fail-fast。落轮 grep 复核
无占坑。
