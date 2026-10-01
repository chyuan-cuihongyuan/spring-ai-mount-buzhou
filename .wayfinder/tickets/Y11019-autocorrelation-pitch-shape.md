---
id: Y11019
title: Y 会话 10 AutocorrelationPitch 自相关基音 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

AutocorrelationPitch（core/metrics，静态纯函数面）：detectPitch(x,fs,fMin,fMax)——归一化自相关滞后域 [fs/fMax, fs/fMin] 峰值 lag→fs/lag；时长不足/频域反序 fail-fast。
