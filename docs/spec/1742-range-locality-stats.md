# Spec 1742 — 范围读局部性分类读面（effort #1742，R43）（effort #1742，R43）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2685–T2686，impl 1342，impl RocksDB 块缓存局部性）。借鉴：范围读偏移是顺序连续还是随机跳跃无分类：顺序可预取缓存友好、随机是碎片化访问信号——读模式画像缺位。

## Problem Statement

`RangeLocalityStats`（spill，实例面线程安全）：record(offset, length)（负值忽略）逐次分类——offset==上一读终点=顺序、否则随机、首读独立；census（reads/sequential/random/sequentialShare 可判对<2 哨兵 −1）。纯读面 opt-in。

## Solution

作为存储调参者，顺序占比高 → 预读窗口可以加大。

## User Stories

1. 17420
2. 17421
3. 17422

## Implementation Decisions

- 17423

## Testing Decisions

- 17424

## Out of Scope

- 17425

## Further Notes

- 17426
