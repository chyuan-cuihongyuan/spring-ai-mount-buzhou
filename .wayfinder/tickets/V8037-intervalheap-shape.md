---
id: V8037
title: V 会话 V19 IntervalHeap 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-30
---

## Question

双端最值怎么单存储 O(log n)？（spec 8018 / effort #8018 / V19）

## Resolution

**IntervalHeap（core/concurrent）**：min-max 层交错隐式树；
offer 上滤定层+pollMin/pollMax 下滤+双 peek O(1)；空堆
null 诚实缺省。
