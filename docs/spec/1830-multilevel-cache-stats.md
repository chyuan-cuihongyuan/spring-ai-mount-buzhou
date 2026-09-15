# Spec 1830 — 多级缓存命中读面（effort #1830，R31）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2861–T2862，impl 1431）。借鉴：
> Caffeine multi-level / CPU L1-L2 层级——逐层命中率分读，L1 失职率单独
> 成指标（L2 命中里本可 L1 拦下的份额）。

## Problem Statement

进程内缓存（L1）+ 共享 store（L2）两级只有合并命中率一个读数：L1 形同虚设
（全漏到 L2）与 L2 无效（全回源）在合并口径里可能同形——该升 L1 容量、该
查预热失效、还是该查键口径，无从分诊。

## Solution

`MultiLevelCacheStats`（buzhou-resilience/cache，静态纯函数）：

- `report(l1Hits, l2Hits, misses)` → `CacheReport(requests, l1Hits, l2Hits,
  misses)`（紧凑构造器核契约：非负 + 四路合计 = requests）；
- `l1HitRate()` / `combinedHitRate()` / `l1DerelictionRate()`（L1 失职率 =
  L2 命中占非回源比——全 miss 时 -1 哨兵；零请求三率 -1 哨兵）。

## User Stories

1. 作为缓存架构师，L1 命中 60%/联合 90%/失职 33% → L1 撑了门面但三分之一
   漏穿，该查预热或键局部性。
2. 作为容量治理者，L1 命中贴地 + L2 高 → L1 容量升档；全层漏穿 → 键口径
   错了（缓存白建）。
3. 作为框架宿主，层级口径自声明，纯读面零状态。

## Implementation Decisions

- 纯读面零状态；失职率分母取「非回源」（回源不归 L1 管）。

## Testing Decisions

- 三率分读；极端三档（全 L1/全 L2/全 miss）异读；零请求哨兵；负数与
  失恒 fail-fast。

## Out of Scope

- 不实现缓存本体；不做层级配置建议自动化。

## Further Notes

- 与 ResponseCacheStore（单级）正交：这是层级账面。
