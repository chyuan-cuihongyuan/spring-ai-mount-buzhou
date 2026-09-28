---
id: T6271
title: T 会话 T36 周期对账的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

Wave 6 五新类型怎么入快照封账？（spec 6035 /
effort #6035 / T36）

## Resolution

**快照补登**：regenerateSnapshot 全 reactor 再生
（1233→1238：BuddyAllocator——memory + ExternalMergeSort——
fs + ExtendibleHashing——metrics + ElevatorScan——policy +
ArenaAllocator——memory）+ api-surface.md 同步 + CONTEXT
计数 +5 + 全仓 verify 三门 + 台账核账。
