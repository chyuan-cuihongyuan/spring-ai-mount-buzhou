# Spec 1869 — 流式中位数保持器（effort #1869，R70）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2939–T2940，impl 1470）。借鉴：
> 双堆中位数经典结构（大顶+小顶对半）——O(log n) 入列 O(1) 取中位，
> 流式到达不排序。

## Problem Statement

「当前中枢」（延迟/评分的中位数）要随观测流更新：排序法每查一次
 O(n log n) 全排；截尾均值/均值都无抗离群——流式的中位数保持缺结构。

## Solution

`MedianKeeper`（core/metrics，synchronized 小临界区）：

- `add(value)`：先进小半再平衡（不变量：小半堆顶 ≤ 大半堆顶、大小差
  ≤ 1 且小半可多一）；
- `median()`：奇数取小半堆顶、偶数取两堆顶均值；空 -1 哨兵；
- 样本有限实数契约（NaN/Inf fail-fast）。

## User Stories

1. 作为指标作者，万级延迟流 add 一次查一次——中位数始终 O(1) 可读，
  不积攒全量重排。
2. 作为观测治理者，单侧长尾不拉走中位（对照均值被 2M 样本拉爆）。
3. 作为框架宿主，样本口径自声明，线程安全即用。

## Implementation Decisions

- synchronized 小临界区（双堆翻转原子）；「小半多一」约定使奇数中位
  直读堆顶零分支。

## Testing Decisions

- 奇偶两态；乱序流=排序流（到达序无关）；偏置流中位抗性；空哨兵与
  NaN/Inf fail-fast。

## Out of Scope

- 不做分位数泛化（LogBucketHistogram 已有近似面）；不做有界窗（滑窗
  中位归未来静脉）。

## Further Notes

- 与 TrimmedMean 互补：那是批式稳健中枢，这是流式中枢。
