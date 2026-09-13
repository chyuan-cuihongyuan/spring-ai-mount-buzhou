# 909 — 评估分数分位数读面

> 来源：I 会话第 10 轮 = effort #909（[T1269](../../.wayfinder/tickets/T1269-percentiles-shape.md) / [T1270](../../.wayfinder/tickets/T1270-percentiles-verify.md) / impl 662）。借鉴：numpy [percentile](https://numpy.org/doc/stable/reference/generated/numpy.percentile.html)（R-7 默认口径）——分布读数的标准问法。

## Problem Statement

`EvalScoreAnalytics` 的分布读数只有 min/max/mean（Report）、反事实阈值（passesAtThresholds）、bootstrap 区间（spec 903）。长尾问法「P50/P95 多少」无现成读面——mean 掩盖长尾，阈值反事实要先猜阈值。

## 目标

- `EvalScoreAnalytics.percentiles(double[] samples, double... quantiles)`：
  - R-7 线性插值（`h = (n−1)·q`；`lower = ⌊h⌋`；结果 = `sorted[lower] + (h−lower)·(sorted[lower+1]−sorted[lower])`——numpy/Excel 默认口径，显式入档）；
  - 返回 `LinkedHashMap<Double,Double>`：q → 分位值（按入参序）；
  - 校验：samples 非空（NaN 元素 fail-fast——spec 903 同纪律）、quantiles 非空且每个 ∈ 开区间 (0,1)；
  - 单样本退化为恒值（n=1 时 h=0 恒插值到自身——自然语义无需特判）；
- 纯函数不触 store；既有 Report/passesAtThresholds/bootstrapMeanInterval 零变化。

## 兼容性

纯增量：公共类新增静态方法，零既有行为变化。
