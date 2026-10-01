---
id: X10075
title: X 会话 38 DctType2 正交 DCT-II 变换 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

DctType2（core/metrics，静态纯函数面）：transform(x) 正交归一 DCT-II（X[0] 均衡尺度 sqrt(1/N)、余弦基 sqrt(2/N) cos(π(2n+1)k/(2N))）；任意长度 O(n²) 直接法；null/空/非有限值 fail-fast。
