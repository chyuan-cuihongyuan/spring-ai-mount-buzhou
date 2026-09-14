---
id: T2423
title: R37 校准系数建议的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2422
created: 2026-09-15
---

## Question

N 会话第 37 轮：偏差修正做自动热调还是建议值？

## Resolution

选 **建议值**。闭环自动调（观察偏差→改系数→再观察）有振荡风险且改的是
预算口径（行为面大）；一阶换算的建议值给足决策依据，宿主判断采纳时机。
样本门槛（minSamples）防噪声建议。
