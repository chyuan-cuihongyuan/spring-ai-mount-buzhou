---
id: T6207
title: T 会话 T4 Sparse Table 稀疏表的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-25
---

## Question

静态序列区间最值怎么查询 O(1)？（spec 6003 /
effort #6003 / T4）

## Resolution

**SparseTable（core/metrics）**：Bender-Farach 思想——倍增
表 O(n log n) 预计算不可变，查询取覆盖区间的两个 2^k 块
min（幂等聚合允许重叠，O(1)）；size 读数；null/空/倒置/
越界 fail-fast。
