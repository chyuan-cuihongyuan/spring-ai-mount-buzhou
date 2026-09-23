# Spec 4012 — 区块 min/max 剪枝（effort #4012，R13）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6025–T6026，impl 2113）。
> 借鉴：DuckDB zone map / Parquet row group 统计 / ORC min-max 索引。

## Problem Statement

列存/日志分块查询「读不读这块」缺统计裁决——全块扫描把 IO 花在
不可能命中的块上。

## Solution

`ZoneMapPruner`（core/cleanup，区块统计谓词裁剪）：

- Zone(id, min, max, nullCount) 三统计；区间重叠判定双侧**含等**
 （min≤high ∧ max≥low——擦边保守读，漏读即错读）；
- zonesOverlapping/zonesMatching/zonesWithNulls 三裁决面；
- zonesSkipped/pruningRatio 剪枝账（无块 NaN 诚实）；
- 构造期校验（min≤max、nullCount≥0、id 非空）fail-fast。

## User Stories

1. 作为存储作者，查询先对 O(块数) 统计裁决再花 O(块体积) IO。
2. 作为优化审计者，剪枝率可量化（局部性好的列 >2/3 免读）。

## Testing Decisions

- 三块点查命中/沟里全剪/边界含；区间剪枝 + 比率 1/3 与全剪 1.0；
  擦边双侧（31–35 全剪 vs 30–40 两块保守读）；空值面单块显形；
  畸形六型 fail-fast。

## Out of Scope

- 不做块内二级索引（归块格式）；不做字符串/多列统计
 （本件 long 数值面）；不做 bloom 过滤器（后续候选静脉）。

## Further Notes

- 与 SweepLineIntervals 正交（读不读 vs 叠几层）。
- 里程碑：13/50。
