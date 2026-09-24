# Spec 5046 — TinyLFU Admission 准入策略（effort #5046，S47）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6193–T6194，impl 2197）。
> 借鉴：Caffeine W-TinyLFU 准入面（4 位 sketch+老化思想）。

## Problem Statement

缓存准入的病：新键无条件挤掉受害者（一次性扫描冲垮
缓存——命中率崩塌）或频率表被历史热点占死（新热点
永远进不来）——**频次裁决+老化面**缺失。

## Solution

`TinyLfuAdmission<K>`（core/cache）：

- 4 位饱和计数 sketch：两行 count-min（估计取两行最小
  抑制碰撞高估），`record` 饱和加；
- `admit(candidate, victim)`：候选估计 ≥ 受害者估计才
  准入——热者不让位、冷者不挤占；
- **老化**：记录数达阈值（列数半）全体减半——旧热点
  平稳淡出、sketch 不被历史占死；
- 读数：estimate/resetCount/sinceReset/resetEvery；
- fail-fast：expectedKeys<1、null 键。

## User Stories

1. 作为缓存作者，扫描键（冷）不再挤掉热键——准入有据。
2. 作为调参作者，resetCount/sinceReset 读数——老化节奏
   可观测。

## Testing Decisions

- 大容量（1024）单调升到饱和 15、他键零估计；准入方向
  （热进冷拒、追平即进）；小容量（16/阈 8）：5 记估 5、
  第 8 记触发老化自增后减半（5+3=8→4）、resetCount=1、
  sinceReset 归零；参数/键 null fail-fast。

## Out of Scope

- 不做窗口段（W-TinyLFU 的 window LRU 段由 SlruCache
  承接）；不做布隆过滤器预滤（Caffeine 门花）；不做
  驱逐执行（本件只裁决准入）。

## Further Notes

- 与 SlruCache（spec 5039）同族不同面：准入裁决 vs 双段
  驱逐。Wave 8 第五件。
- 里程碑：S47/50（94%）。
