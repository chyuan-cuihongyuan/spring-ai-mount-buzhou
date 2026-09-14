---
id: T2386
title: R18 梯度式自适应并发的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2385
created: 2026-09-15
---

## Question

N 会话第 18 轮：如何验收？

## Resolution

GradientAdaptiveLimiterTest 七断言：劣化乘性下调（先加性上涨脱离 minLimit 再
断降——零失败前提钉死）；变快加性上涨 + gradient 读数；容差带不动；warmup
只学不调；tryAcquire/release 联动；封顶 maxLimit；配置校验 fail-fast。
