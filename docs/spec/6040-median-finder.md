# Spec 6040 — Median Finder 双堆中位数流（effort #6040，T40）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6279–T6280，impl 2240）。
> 借鉴：流式中位数双堆经典思想。

## Problem Statement

流式中位数的病：每次查询全量排序（O(n log n) 每查放大）——
**双堆夹逼 O(log n) 插入 O(1) 查询面**缺失。

## Solution

`MedianFinder`（core/concurrent，源码已预载）：

- 最大堆保左半+最小堆保右半，插入经再平衡（大小差 ≤1）；
- median 奇数取大堆顶、偶数取两顶均值（无符号右移防溢出，
  奇偶显式两态）；O(1) 查询；
- 读数：size/isEmpty；空返回 null（不抛——流未开始诚实）。

## User Stories

1. 作为行情作者，百万点流滚动中位数零排序——延迟底座。
2. 作为审计作者，同流同中位数序列——确定性可回放。

## Testing Decisions

- 500 随机流每步与排序圣像全等；奇偶两态；固定流序列锚
 （3,2,3,2,3,3,3,3）；MIN/0/MAX 极值；空 null；确定性。

## Out of Scope

- 不做滑动窗中位数（全流累积定构）；不做删除。

## Further Notes

- 与 IndexedHeap（6026）同族不同面：单序堆+位置映射 vs
  双堆夹逼中位数。
- 里程碑：T40/50（80%）。
