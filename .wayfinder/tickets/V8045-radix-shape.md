---
id: V8045
title: V 会话 V23 RadixSorter 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-30
---

## Question

整数排序怎么线性扫？（spec 8022 / effort #8022 / V23）

## Resolution

**RadixSorter（core/metrics）**：非负 long LSD 每轮 8 位
256 桶计数稳定分桶 O(n·w)；值语义副本；负数 fail-fast。
