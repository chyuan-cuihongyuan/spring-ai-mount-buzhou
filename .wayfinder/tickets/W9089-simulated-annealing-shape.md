---
id: W9089
title: W 会话 W45 Simulated Annealing 模拟退火 的形状裁决
type: task
status: closed
assignee: zcode-w
blocked-by: []
created: 2026-09-30
---

## Question

形状怎么定？

## Resolution

SimulatedAnnealing（core/policy）：exp(−Δ/T) 接受+几何降温+best-ever 最优化器。
