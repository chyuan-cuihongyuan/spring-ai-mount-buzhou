---
id: T5057
title: Q 会话 R29 CLOCK 驱逐的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

近似 LRU 怎么 O(1) 低摩擦？（spec 3028 / effort #3028 / R29）

## Resolution

**ClockEviction（core/cache，泛型）**：CLOCK 二次机会——环形帧阵
列+引用位，命中置位零搬移；满载时针扫描位 1 清位跳过（二次机会）
位 0 逐出。FIFO 盲逐热点病与严格 LRU 链表搬移贵的中间档。
evictedCount 对账读数。
