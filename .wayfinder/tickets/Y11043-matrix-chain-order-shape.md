---
id: Y11043
title: Y 会话 22 MatrixChainOrder 矩阵链区间 DP 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

MatrixChainOrder（core/concurrent，静态纯函数面）：minMultiplications(dims)——区间 DP m[i][j]=min m[i][k]+m[k+1][j]+d_{i−1}d_kd_j；n<2/维度<1 fail-fast。
