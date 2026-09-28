---
id: U7287
title: U 会话 U44 柱状图最大矩形的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

最大矩形怎么免 O(n²)？（spec 7043 / effort #7043 / U44）

## Resolution

**LargestRectangleHistogram（core/concurrent）**：单调递增栈左右边界 O(n)；哨兵 0 清栈；long 域。
