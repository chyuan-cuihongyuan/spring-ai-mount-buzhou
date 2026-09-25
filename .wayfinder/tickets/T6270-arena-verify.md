---
id: T6270
title: T 会话 T35 Arena Allocator 竞技场分配器的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6269]
created: 2026-09-26
---

## Question

T35 合同怎么逐一验绿？（spec 6034 / effort #6034 / T35）

## Resolution

**验证通过**：ArenaAllocatorTest 四测全绿——顺序偏移确定
性；freeAll 归零+峰值保留；读写区域+回收清零；fail-fast。
