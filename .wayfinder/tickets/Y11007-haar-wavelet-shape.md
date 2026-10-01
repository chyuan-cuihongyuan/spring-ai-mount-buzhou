---
id: Y11007
title: Y 会话 4 HaarWavelet 小波 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

HaarWavelet（core/metrics，静态纯函数面）：forward(x) 正交 Haar 全分解（对 (a,d)=((x₀+x₁)/√2,(x₀−x₁)/√2) 逐层折半至 1，近似前置树状布局）；Parseval 严格守恒；null/空/非 2 幂/非有限 fail-fast。
