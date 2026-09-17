---
id: T5031
title: Q 会话 R16 Fenwick 树的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

流式频次表怎么点更新与前缀秩查询双 O(log n)？（spec 3015 / effort #3015 / R16）

## Resolution

**FenwickTree（core/metrics）**：lowbit 区间分解（Fenwick 1994）——
add/prefixSum 双 O(log n)（朴素数组更新/查询两难的根治）+
rangeSum=前缀差+total+0-based 公共面（内部 1-based）+long 可负
累加+越界 fail-fast。延迟分桶直方秩查询类读数的单件化地基。
