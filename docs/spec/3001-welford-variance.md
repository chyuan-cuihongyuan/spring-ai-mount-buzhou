# Spec 3001 — Welford 在线方差（effort #3001，R2）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5003–T5004，impl 2002）。
> 借鉴：Welford 增量算法 / Chan et al. 并合并——单遍稳定流式统计。

## Problem Statement

流式指标的均值/方差若按定义式（Σx 与 Σx²）在线累计，大偏移 + 小
波动场景（1e9 量级延迟基线上的毫秒抖动）两个大数相减灾难性精度
抵消——方差读数失真甚至为负；分片并行统计又缺合并口径。

## Solution

`WelfordAccumulator`（core/metrics，可变累积器单流口径）：

- `add(x)` 单遍递推（mean/m2 增量，delta 双用——先修均值再修矩）；
- `merge(other)` Chan pairwise 合并（分片 ↔ 单遍全量数学等价，
  空侧双向恒等、交换律成立）；
- 双分母口径：`sampleVariance()`（n−1）/ `populationVariance()`（n）；
- 诚实边界：空累积器 mean/方差 NaN；单点 sampleVariance NaN。

## User Stories

1. 作为指标作者，延迟分布的均值/方差单遍流式累计不因量纲偏移失真。
2. 作为分片统计作者，分片各自 Welford 后 merge 等价全量单遍。

## Testing Decisions

- 教材集 {2,4,4,4,5,5,7,9} 手算（mean=5、样本方差 32/7、总体 4）；
- 空态/单点 NaN 边界；分片 merge == 全量（均值双方差三对账）；
- merge 交换律；merge 空向双恒等；1e9 偏移 + {1,2,3} 抖动紧公差
  （Welford vs 朴素公式稳定性的定量证据）。

## Out of Scope

- 不做滑动窗（窗口口径归 SlidingExtremum 族后续轮）；不做加权样本；
  不接 metrics 导出（接线归后续轮）。

## Further Notes

- 与 FiveNumberSummary（全量分位）/ EwmaEstimator（指数遗忘）成
  分布刻画三件：全量分位 / 遗忘均值 / 精确方差。
- 里程碑：2/150。
