---
id: T6253
title: T 会话 T27 Indexed Heap 索引堆的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

就绪队列怎么 O(1) 定位更新？（spec 6026 /
effort #6026 / T27）

## Resolution

**IndexedHeap（core/concurrent，源码 T24 预载）**：id→堆位
哈希映射 O(1) 定位+updatePriority 双向上浮/下沉；同 id 唯一、
堆序确定性（同优先级 id 小先）；重复/缺席/空堆 fail-fast。
