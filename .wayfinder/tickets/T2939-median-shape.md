---
id: T2939
title: 流式中位数保持器的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

「当前中枢」怎么随观测流 O(1) 可读？（spec 1869 / effort #1869 / R70）

## Resolution`

**双堆（大顶+小顶对半）经典结构 `MedianKeeper`（core/metrics）**：
add O(log n)（先进小半再平衡——不变量：小半堆顶≤大半堆顶、差≤1 小半
可多一）+ median O(1)（奇数小半堆顶/偶数两顶均值，空 -1 哨兵）+
size；synchronized 小临界区；NaN/Inf fail-fast。排序法每次 O(n log n)
的流式中枢替代。

