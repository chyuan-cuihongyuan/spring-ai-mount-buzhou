# Spec 1858 — 频次衰减竞速（effort #1858，R59）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2917–T2918，impl 1459）。借鉴：
> Redis LFU counter decay——访问计数周期性减半，老热点自然退烧、新热点
> 可顶替；顶替周期即缓存自适应性读数。

## Problem Statement

频次计数只有涨没有退：昨天的热点赖在缓存顶（计数只增不减）、今天的新
热点永远追不上（起步计数低）——「老热点该退多快、新热点多久能顶上」的
衰减竞速没有读数，缓存自适应性无从调。

## Solution

`FrequencyDecay`（core/cache，静态纯函数）：

- `decayed(counter, periods)`：每周期减半（向下取整、封底 0）；
- `overtakePeriod(counter, newHitsPerPeriod)`：最小 t 使 命中×t &gt;
  decayed(counter, t)——新热点顶替老热点的周期数（零命中/不可达 -1
  哨兵；MAX_RACE_PERIODS 保险丝）。

## User Stories

1. 作为缓存调优者，老计数 100、新热点 5 命中/周期 → 顶替周期 3——
   热点换代 3 周期完成，符合预期；周期过长该加衰减率。
2. 作为容量作者，衰减竞速纯数学可回放——调参不靠玄学。
3. 作为框架宿主，周期口径（分钟/小时）自声明。

## Implementation Decisions

- 纯函数（确定性——对照 Redis 概率增量的随机口径）；衰减减半向下取整。

## Testing Decisions

- 衰减序列（100→50→25→12）+封底；顶替周期三档（3/1/7 代码算出）；零
  计数即刻顶替与零命中永不过顶；畸形四型 fail-fast。

## Out of Scope

- 不做概率对数增量（Redis lfu_log_factor 归未来静脉）；不接缓存驱逐。

## Further Notes

- 与 SemanticCacheStore 的 LFU 正交：那是驱逐策略，这是衰减-顶替数学面。
