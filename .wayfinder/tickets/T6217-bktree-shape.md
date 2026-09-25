---
id: T6217
title: T 会话 T9 BK 树的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

编辑距离邻域查询怎么不全库扫？（spec 6008 /
effort #6008 / T9）

## Resolution

**BkTree（core/metrics）**：Burkhard-Keller 思想——节点按到
父词距离分叉，查询只下探距离 ∈ [d−r, d+r] 分支（三角不等式
剪枝不漏）；动态 add、d=0 幂等、query 字典序 canonical 输出；
size 读数；null 词/r<0 fail-fast。
