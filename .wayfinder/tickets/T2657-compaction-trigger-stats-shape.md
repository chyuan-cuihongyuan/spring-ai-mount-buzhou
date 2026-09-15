---
id: T2657
title: 压缩触发原因分布的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

CompactionTriggerStats 的形状怎么裁决？（spec 1728 / effort #1728 / R29）（spec 1728 验收/裁决）

## Resolution

Trigger 四闭集 IDLE/RATIO/MANUAL/CHECKPOINT+record+census+idleShare −1 哨兵+resetForTest——RocksDB/Cassandra compaction stats 思想，与 CompactionRatioStats 互补。
