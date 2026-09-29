---
id: V8039
title: V 会话 V20 LeftistHeap 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-30
---

## Question

堆合并怎么摆脱全量重排？（spec 8019 / effort #8019 / V20）

## Resolution

**LeftistHeap（core/concurrent）**：npl 左≥右+右路径交换
合并 O(log n)；merge/offer/poll/peek 四面；npl 不变量可
全量校验。
