---
id: T6269
title: T 会话 T35 Arena Allocator 竞技场分配器的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

高频小分配怎么免 GC 压力？（spec 6034 /
effort #6034 / T35）

## Resolution

**ArenaAllocator（core/memory，源码 T30 预载）**：线性 bump
水位指针 O(1) 分配+freeAll 整池归零+highWaterMark 峰值
审计（不被回收清零）；单块 free 显式抛出（诚实边界）；
capacity≤0/size≤0/越界 fail-fast。
