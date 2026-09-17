---
id: T5069
title: Q 会话 R35 Croston 预测器的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

稀疏零海序列的偶发需求怎么预测？（spec 3034 / effort #3034 / R35）

## Resolution

**CrostonForecaster（core/metrics）**：Croston 1972——需求大小与
需求间隔分开平滑（仅非零观测更新），预测率=z'/p'——「毛刺多大
不重要，率才是口径」。朴素指数平滑被零海拖向零病的根治；全零
NaN 诚实+稀疏度对账面。
