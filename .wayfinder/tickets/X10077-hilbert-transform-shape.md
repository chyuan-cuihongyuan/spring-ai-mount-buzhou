---
id: X10077
title: X 会话 39 HilbertTransform 解析信号包络 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

HilbertTransform（core/metrics，静态纯函数面）：envelope(x)——FFT 频域单边化（h[0]=h[n/2]=1、正频 ×2、负频 0）+逆 FFT 共轭技巧取模；消费 FftIterative（10036）流程内自组合；2 幂长约束 fail-fast。
