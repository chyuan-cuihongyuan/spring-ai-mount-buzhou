# Spec 5044 — Segment Tree 线段树（effort #5044，S45）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6189–T6190，impl 2195）。
> 借鉴：线段树经典（ZKW 2n 迭代式数组实现思想）。

## Problem Statement

序列区间聚合的病：区间和每次线性扫（区间查询 O(n) 放大）
或前缀和数组（点更新 O(n) 重算）——**两者兼得的对数面**
缺失。

## Solution

`SegmentTree`（core/metrics）：

- 2n 迭代式数组：叶 [n..2n) 存原值、父存子区间和（无需
  递归与 4n 空间）；`rangeSum [from,to]` O(log n)、
  `update(index,newValue)` 自叶向上回填 O(log n)；
- 读数：size；
- fail-fast：null/空数组、区间越界/倒置、下标越界。

## User Stories

1. 作为监控作者，任意子区间和 O(log n)——面板直方图
   可下钻。
2. 作为流式作者，点更新不重算全前缀——写入对数成本。

## Testing Decisions

- 16 元素（含负值）全部 136 个子区间 vs 暴力扫圣像逐个
  全等；点更新两处后全区间+子区间仍等暴力；单元素树；
  负值求和；区间越界/倒置/下标越界 fail-fast。

## Out of Scope

- 不做惰性传播区间改（本件是点更新+区间查面）；不做
  max/min 聚合变体；不做持久化版本。

## Further Notes

- 与 FenwickTree（metrics）同族不同面：前缀和 BIT vs
  任意区间和+点更新；与 IntervalTree（spec 5022）不同面：
  stabbing 区间集合 vs 序列区间聚合。Wave 8 第三件。
- 里程碑：S45/50（90%）。
