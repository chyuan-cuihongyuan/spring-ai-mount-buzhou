---
id: T2195
title: 事件去重聚合读面（EventDeduplicator 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 48 轮（换题轮）：事件去重压力聚合面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：FeedbackExporter 画像面过窄——换入 EventDeduplicator 聚合轴（仅全局 counter 无读面）。

形状裁决：EventDeduplicator 实例面增量——passedCount/dedupedCount 双计数（守恒 seen=passed+deduped）+DeduplicationStats(ringSize/capacity+deduplicationRatio 派生 -1 哨兵)+resetStatsForTest 只清计数不清环（去重语义不被复位破坏）；onEvent 语义逐位不变。

Out of scope：类型分桶；容量动态调整；跨实例聚合。
