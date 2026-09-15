# Spec 1743 — spill 句柄驻留年龄直方（effort #1743，R44）（effort #1743，R44）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2687–T2688，impl 1343，impl Redis OBJECT IDLETIME）。借鉴：spill 句柄驻留年龄无分布：老句柄堆积=onload 回收跟不上 offload，盘上驻留成本与丢失风险同涨。

## Problem Statement

`SpillHandleAgeHistogram`（spill，实例面线程安全桶式房规）：默认 1m/1h/1d 四桶+eldestMillis 哨戒+负值忽略。与 SpillUsage（用量面）互补。纯读面 opt-in。

## Solution

作为回收调参者，更老桶堆积 → onload 策略该激进些。

## User Stories

1. 17430
2. 17431
3. 17432

## Implementation Decisions

- 17433

## Testing Decisions

- 17434

## Out of Scope

- 17435

## Further Notes

- 17436
