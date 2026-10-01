---
id: X10081
title: X 会话 41 LombScargle 不均匀采样频谱 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

LombScargle（core/metrics，静态纯函数面）：periodogram(times,values,fMin,fMax,count)——经典归一 Lomb–Scargle（τ 相位偏移 atan2 中心化+2σ² 归一）；正频域网格扫频；null/长度不配/非正频/越序频界 fail-fast。
