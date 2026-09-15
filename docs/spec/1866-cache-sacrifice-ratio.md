# Spec 1866 — 缓存牺牲率（effort #1866，R67）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2933–T2934，impl 1467）。借鉴：
> 体系结构缓存分析惯例（sacrifice ratio / thrashing）——插入驱逐比与
> 命中率合诊颠簸：缓存太小，插入的还没用就被挤走。

## Problem Statement

缓存健康只有命中率一读数：满缓存正常换血（牺牲高但命中也高）与真
 颠簸（牺牲高且命中低——白忙）在命中率里同形——「该扩容」与「工作集
 本就散」处置相反却无分诊面。

## Solution

`CacheSacrificeRatio`（buzhou-resilience/cache，静态纯 record）：

- `CacheAccount(inserts, evictionsOnInsert, hits, misses)` 契约构造；
- `sacrificeRatio()` = 插入期驱逐/插入（零插入 -1 哨兵；全联理想 0）；
- `hitRate()`（零查找 -1 哨兵）；
- `thrashing(sacrificeThreshold, hitThreshold)` 双条件：牺牲率 ≥ 阈 且
  命中率 < 阈——单看牺牲率是正常换血、单看命中低是工作集散。

## User Stories

1. 作为容量作者，牺牲 0.8 + 命中 0.3 → 颠簸白忙——扩容，不是调 TTL。
2. 作为运维者，牺牲 0.8 + 命中 0.9 → 满缓存换血正常——不动。
3. 作为框架宿主，四计数口径自声明，纯读面零调参。

## Implementation Decisions

- 纯读不调参；双条件分诊（单条件各有假阳性面）。

## Testing Decisions

- 全联理想；颠簸双过 vs 换血不触发；哨兵两型；畸形三型 fail-fast。

## Out of Scope

- 不做容量建议数值（多少才够归未来静脉）；不接驱逐器。

## Further Notes

- 与 MultiLevelCacheStats 互补：那是层级命中，这是插入驱逐与颠簸合诊。
