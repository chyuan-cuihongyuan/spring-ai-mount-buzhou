---
id: T3227
title: 层级令牌桶的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

两级限流的父顶硬顶+子桶隔离怎么原语化？（spec 2063 / effort #2063 / R64）

## Resolution

**Linux HTB 线程安全两级桶 `HierarchicalTokenBucket`（core/backpressure）**：
registerChild 子桶（合容量可超父顶——父顶兜底）+tryConsume **父剩余
×子剩余双闸**（父空总额硬顶子有币借不到；子空自限父富余不给）双扣
+refill 双封顶+snapshot 对账面——「子各自限额但合打不破父总额」。
