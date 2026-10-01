---
id: Y11015
title: Y 会话 8 Cepstrum 倒频谱 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

Cepstrum（core/metrics，静态纯函数面）：realCepstrum(x)——FFT 对数幅度谱 IFFT 取实（log(mag+ε) 防零）；消费 FftIterative（10036）流程内自组合；2 幂长约束+null/非 2 幂/非有限 fail-fast。
