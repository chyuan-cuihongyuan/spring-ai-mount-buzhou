---
id: Y11025
title: Y 会话 13 YuleWalker 方程 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

YuleWalker（core/metrics，静态纯函数面）：solve(covariances)——含 r0 自协方差域 AR 拟合（r0 归一化后消费 DurbinLevinson（11010）流程内自组合+σ²=r0−Σa_j r_j 复原）；r0≤0 fail-fast。
