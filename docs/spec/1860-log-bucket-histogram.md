# Spec 1860 — 对数分桶直方图（effort #1860，R61）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2921–T2922，impl 1461）。借鉴：
> HdrHistogram/DDSketch——对数指数桶界分位数，桶内相对差有界 → 估计
> 误差有界，O(n) 免全排序。

## Problem Statement

延迟/成本分位数（P50/P95/P99）现靠全排序（O(n log n)）或手搓线性桶
（高段失真——100ms 与 10s 同桶）——「相对误差有界 + 一遍分桶」的近似
分位数基建缺位。

## Solution

`LogBucketHistogram`（core/metrics，静态纯函数）：

- `bucketIndex(value, gamma)` = floor(ln v / ln γ)（桶 k 覆盖
  [γ^k, γ^(k+1))——桶内相对差 ≤ γ−1）；
- `buckets(samples, gamma)` 分桶账（TreeMap 升序）；
- `quantile(samples, q, gamma)`：桶序累计到 q×n，取桶几何中点
  sqrt(γ^k·γ^(k+1)) 为估计 → `ApproxQuantile(estimate,
  relativeErrorBound=γ−1)`；
- 正值域契约（对数域）；DEFAULT_GAMMA=1.25（±12.5% 量级）。

## User Stories

1. 作为指标作者，1000 样本 P95 一遍分桶出估计——误差界声明在返回值里
  （0.25 上界保守，实测远小）。
2. 作为容量作者，γ 调 1.1 换精度、调 1.5 换桶数——精度/内存显式权衡。
3. 作为框架宿主，正值口径（毫秒/字符）自声明。

## Implementation Decisions

- 纯函数（TreeMap 桶序确定性）；几何中点（桶内对数对称）。

## Testing Decisions

- 桶号边界（γ=2 四点+次单位值负桶）；P95 实际误差 < 界（1000 均匀
  样本对照精确最近秩）；q 两端可用；正值域与畸形五型 fail-fast。

## Out of Scope

- 不做合并语义（跨实例 merge 归 DDSketch 未来静脉）；不接 Micrometer。

## Further Notes

- 与 TurnLatencyPercentiles 正交：那是精确百分位，这是有界近似面。
