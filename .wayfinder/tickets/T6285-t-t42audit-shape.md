---
id: T6285
title: T 会话 T42 周期对账的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

Wave 7 五新类型怎么入快照封账？（spec 6042 /
effort #6042 / T42）

## Resolution

**快照补登**：regenerateSnapshot 全 reactor 再生
（1238→1244：Wave 7×5 CountingBloomFilter/StableBloomFilter——metrics
+ WeightedReservoirSampler——policy + MedianFinder——
concurrent + AimdWindow——ratelimit）+ api-surface.md 同步
+ CONTEXT 计数 +5 + 全仓 verify 三门 + 台账核账。
