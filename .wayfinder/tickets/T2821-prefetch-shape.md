---
id: T2821
title: Prefetch 信用窗口的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

在飞上限口径的背压闸怎么安放？（spec 1810 / effort #1810 / R11）

## Resolution

**RabbitMQ basic.qos / AMQP credit-based flow control 思想
`PrefetchCreditWindow`（core/backpressure）**：容量 ≥1 fail-fast；
tryAcquire 满窗即拒（拒绝计入 totalExhausted 流控压力读数）；release 确认
归还（空窗归还 fail-fast）；stats 快照（capacity/inFlight/available/
totalExhausted + utilization）。与令牌桶（速率）互补：在飞上限免速率估计，
下游多快上游多快。

