---
id: T6259
title: T 会话 T30 周期对账的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

Wave 5 五新类型怎么入快照封账？（spec 6029 /
effort #6029 / T30）

## Resolution

**快照补登**：regenerateSnapshot 全 reactor 再生
（1228→1233：MpscQueue/StripedLock/IndexedHeap/StrideScheduler/
Mlfq——concurrent×5）+ api-surface.md 同步 +5 行 + CONTEXT
计数 +5 + 全仓 verify 三门 + 台账核账。
