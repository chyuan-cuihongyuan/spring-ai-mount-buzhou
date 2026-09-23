# Spec 4015 — Slab 类装箱（effort #4015，R16）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6031–T6032，impl 2116）。
> 借鉴：Memcached slab allocator（增长因子默认 1.25）。

## Problem Statement

「malloc 混尺寸长跑碎片化」——固定尺寸槽分档的容量规划地基件
缺失（缓冲池/对象池/消息帧池）。

## Solution

`SlabClassPacker`（core/cache）：

- 块尺寸按增长因子几何级数分档（chunkSizeMin 起、maxItemSize
  封顶停档）；item 归**最小容纳档**（恰界归本档不进位）；
- 同档等尺寸切槽——分配/释放永不产生外部碎片（代价是档内
  **内部浪费** chunk−item，wasteRatio 可审计）；超最大块 −1 拒收；
- allocate 分配记账（per-class 计数）供容量审计。

## User Stories

1. 作为池作者，槽档几何规划——长跑零外部碎片。
2. 作为容量审计者，档内浪费与档计数有账可查。

## Testing Decisions

- 96/1.25/1024 十二档表（96/120/150/…/899/1024 封顶）；恰界归档
  三例 + 超块拒收；浪费比 0/23÷120/NaN 三面；记账 per-class +
  拒收不记账；畸形七型 fail-fast。

## Out of Scope

- 不做 slab 页再平衡/重新分配（档界运行期固定）；不做 LRU
 （缓存族已覆盖）；不做线程安全面（记账归调用方聚合）。

## Further Notes

- 与 BucketTableSizing 同族不同面（表容 vs 槽档）。
- 里程碑：16/50。
