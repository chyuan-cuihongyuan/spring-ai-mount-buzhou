---
id: T6254
title: T 会话 T27 Indexed Heap 索引堆的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6253]
created: 2026-09-26
---

## Question

T27 合同怎么逐一验绿？（spec 6026 / effort #6026 / T27）

## Resolution

**验证通过**：IndexedHeapTest 五测全绿——300 随机出序优先级
不降；decrease/increase 双向；混合序列终态逐值；remove 一致；
fail-fast。
