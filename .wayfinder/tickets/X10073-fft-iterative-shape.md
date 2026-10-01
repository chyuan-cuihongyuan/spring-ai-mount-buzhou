---
id: X10073
title: X 会话 37 FftIterative 迭代快速傅里叶 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

FftIterative（core/metrics，静态纯函数面）：transform(re,im) 原位迭代基-2 DIT（位反转置换+蝶形）+magnitudes(realSignal) 便捷面；2 的幂长约束；null/长度不配/非 2 幂/非有限值 fail-fast。
