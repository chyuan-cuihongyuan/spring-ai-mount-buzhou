---
id: Y11017
title: Y 会话 9 ZeroCrossingRate 过零率 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

ZeroCrossingRate（core/metrics，静态纯函数面）：rate(x)——相邻符号变化计数/(n−1)；零值沿用前符号口径（不计过零）；null/空/单点/非有限 fail-fast。
