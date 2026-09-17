# Spec 3015 — Fenwick 树（effort #3015，R16）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5031–T5032， impl 2016）。
> 借鉴：Binary Indexed Tree（Fenwick 1994，lowbit 区间分解）。

## Problem Statement

流式频次表（延迟分桶直方/调用计数槽）要同时支持高频点更新与前缀
秩查询：朴素数组两难——裸槽更新 O(1) 查询 O(n)，前缀缓存查询 O(1)
更新 O(n)。

## Solution

`FenwickTree`（core/metrics，单流口径）：

- lowbit 区间分解：`add(index, delta)` 与 `prefixSum(count)` 双
  **O(log n)**；
- `rangeSum(from, toExclusive)` = 前缀差；`total()`；`size()`；
- 0-based 公共面（内部 1-based）；long 累加（可负——增减双向）；
- 越界/零容量 fail-fast。

## User Stories

1. 作为直方作者，计数槽高频更新与分位秩查询同件化——不再两难。
2. 作为对账作者，随机点更新对拍朴素数组恒等——正确性可证。

## Testing Decisions

- 前缀和手算（{3,5,7,−,2} 阶梯）；负增量回零；区间和=前缀差
  （12/17/0 三例）；1000 随机点更新逐次对拍朴素数组；空/全前缀
  边界与初值全零；六路越界 fail-fast；Long.MAX/2 大值无溢出。

## Out of Scope

- 不做区间更新（lazy 线段树留白）；不做下标压缩/离散化（归调用
  方映射）；不做并发。

## Further Notes

- 与 SlidingExtremum（滑窗极值）/ PSquareQuantile（流式分位）
  互补：本件是**可变槽频次表的秩查询**地基。
- 里程碑：16/150。
