---
id: T2929
title: 可见性超时账的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

「至少一次」投递的时限与上限怎么账面化？（spec 1864 / effort #1864 / R65）

## Resolution`

**AWS SQS visibility timeout 思想纯记账 `VisibilityTimeoutAccounting`
（core/webhook）**：shouldRedeliver（未确认且到点边界含上）+
shouldDeadLetter（重投计数≥上限含上，零容忍合法）+ census 三段普查
（在飞/超时重投/死信候选+最老在飞龄+inFlightRatio 哨兵）。取走即隐藏
防重复、超时回队不丢、穷尽死信不死循环——三段语义两全。

