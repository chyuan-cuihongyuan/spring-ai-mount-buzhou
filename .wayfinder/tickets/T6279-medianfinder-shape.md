---
id: T6279
title: T 会话 T40 Median Finder 双堆中位数流的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

流式中位数怎么 O(log n) 插入 O(1) 查询？（spec 6040 /
effort #6040 / T40）

## Resolution

**MedianFinder（core/concurrent，源码本轮入档）**：最大堆保
左半+最小堆保右半，插入再平衡（差≤1）；median 奇取堆顶/
偶取两顶均值（无符号右移防溢出）；size/isEmpty 读数；空
null；null/极值安全。
