---
id: Y11033
title: Y 会话 17 PlattScaling Platt 校准 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

PlattScaling（core/metrics，静态纯函数面）：fit(scores,labels,iterations)——Newton 迭代最小化交叉熵拟合 p=sigmoid(A·s+B)（嵌套 Platt record(A,B)+probability(score)）；标签 {0,1}/迭代域 fail-fast。
