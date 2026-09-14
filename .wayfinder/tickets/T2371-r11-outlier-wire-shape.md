---
id: T2371
title: R11 离群驱逐接线与分类感知的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2370
created: 2026-09-15
---

## Question

N 会话第 11 轮：发现 ModelOutlierEjection 生产零接线——只做分类感知还是完整接线？

## Resolution

选 **完整接线 + 分类感知**。孤类状态 = 机制等于关闭，分类感知只是锦上添花；
接线才是把 spec 149 的机制价值落地。喂入挂点与 circuit.recordSuccess/
recordTerminal 完全对称（五处成功五处终态失败），过滤挂两处降级候选迭代，
opt-in outlier.enabled 默认零行为。分类集默认与熔断 failure-categories 同口径
（AUTH/CONTENT/RATE_LIMIT 不驱赶端点）。
