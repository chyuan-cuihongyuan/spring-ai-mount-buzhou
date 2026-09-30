---
id: X10021
title: X 会话 X11 Conjugate Gradient 共轭梯度 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

ConjugateGradient（core/concurrent）：UnaryOperator 矩阵向量积接口+相对残差收敛+迭代步读数（Hestenes–Stiefel 思想）。
