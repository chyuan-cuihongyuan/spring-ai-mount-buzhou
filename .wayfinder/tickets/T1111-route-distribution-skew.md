---
id: T1111
title: 路由分布倾斜读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

声明 vs 实际的分布对账读数怎么做？集中度量选什么、缺权/零流量口径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 6 轮 = effort #805 / spec 805 / impl 558）：`RouteDistributionReadout` 双层 Collector+analyze 纯函数——|偏差| 降序（打平典序）、gini 基尼集中度、缺权重期望=0、声明无流量也是偏差行、dominant 读数；只读不纠偏。
