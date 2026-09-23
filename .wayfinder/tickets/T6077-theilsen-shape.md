---
id: T6077
title: R 会话 R39 Theil-Sen 稳健斜率的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

趋势估计怎么对离群点免疫不拉偏？（spec 4038 / effort #4038 / R39）

## Resolution

**TheilSenSlope（core/metrics）**：成对斜率中位数（竖直对
跳过，崩溃点 ≈29%）+ Sen 截距（median(y−slope·x)）；
偶数下中位确定性；纯函数 fit + predict；竖直退化/畸形
fail-fast。
