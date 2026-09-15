---
id: T2861
title: 多级缓存读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

两级缓存的分诊读数怎么安放？（spec 1830 / effort #1830 / R31）

## Resolution

**Caffeine multi-level/CPU L1-L2 思想纯读面 `MultiLevelCacheStats`
（buzhou-resilience/cache）**：CacheReport 构造器核四路合计契约 +
l1HitRate/combinedHitRate/l1DerelictionRate（失职率=L2 命中占非回源比，
全 miss -1 哨兵；零请求三率 -1）。升容量/查预热/查键口径三分诊有据。

