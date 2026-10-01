---
id: X10089
title: X 会话 45 LinearRegression OLS 正规方程 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

LinearRegression（core/metrics，静态纯函数面）：fit(X,y) 截距列内联 1 列扩设计阵+正规方程 (AᵀA)w=Aᵀy 消费 GaussianElimination（10006）流程内自组合；奇异矩阵 fail-fast 上浮。
