---
id: Y11013
title: Y 会话 7 DaubechiesD4 小波 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

DaubechiesD4（core/metrics，静态纯函数面）：forward(x) 四系数正交 D4 全分解（周期延拓卷积+下采样，近似折半至 <4；[最粗近似,细节由粗到细] 布局）；Parseval 守恒+双消失矩线性零细节；null/空/非 2 幂/<4/非有限 fail-fast。
