---
id: Y11009
title: Y 会话 5 GoertzelAlgorithm 单频检测 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

GoertzelAlgorithm（core/metrics，静态纯函数面）：power(samples,f,fs)——二阶复数振子递推 w[n]=x[n]+2cosω·w[n−1]−w[n−2]+模平方读数；f=0/f=fs 边界拒绝；null/非正 fs/频域越界 fail-fast。
