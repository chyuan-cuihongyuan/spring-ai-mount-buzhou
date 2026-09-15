# Spec 1724 — 事实读热分桶（effort #1724，R25）（effort #1724，R25）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2649–T2650，impl 1324，impl Redis LFU 8-bit 频次直方）。借鉴：事实被读的频次分布不可见：少数热点事实承载多数读取还是均匀冷读——缓存与衰减（FactDecayPolicy）调参无依据。

## Problem Statement

`FactReadHistogram`（core/fact，实例面线程安全）：逐键读取计数有界默认 512 超出并 _overflow_ 桶；census() 冷(1)/温(2–4)/热(5–16)/灼(>16) 四档键数+totalReads+distinctKeys；null/空键归 _anonymous_。纯读面 opt-in。

## Solution

作为缓存调参者，灼热桶 5% 键承载 60% 读 → 热事实加缓存层。

## User Stories

1. 17240
2. 17241
3. 17242

## Implementation Decisions

- 17243

## Testing Decisions

- 17244

## Out of Scope

- 17245

## Further Notes

- 17246
