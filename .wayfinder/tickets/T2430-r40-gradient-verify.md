---
id: T2430
title: R40 梯度限流器观测接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2429
created: 2026-09-15
---

## Question

N 会话第 40 轮：如何验收？

## Resolution

GradientLimiterWiringTest 两断言：两次 record(100) 后 View 双 EMA=100 且
gradient=1.0（喂入语义）；install 替换同实例、null 重置默认（limit=4 起步）。
GradientAdaptiveLimiterTest 7 用例零回归。
