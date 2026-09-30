---
id: W9077
title: W 会话 W39 XorShift64 伪随机数 的形状裁决
type: task
status: closed
assignee: zcode-w
blocked-by: []
created: 2026-09-30
---

## Question

形状怎么定？

## Resolution

XorShift64（core/metrics）：13,7,17 三移位异或推进 RNG+无偏有界面。
