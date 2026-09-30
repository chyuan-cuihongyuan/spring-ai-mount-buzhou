---
id: X10015
title: X 会话 X8 LU Decomposition 分解 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

LUDecomposition（core/concurrent）：部分主元 PA=LU 实例面——solve 复用 O(n²)+determinant+防御性拷贝三读数（Doolittle 思想）。
