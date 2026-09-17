# Spec 3037 — lag-k 自相关（effort #3037，R38）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5075–T5076，impl 2037）。
> 借鉴：Box-Jenkins ACF（自相关函数）。

## Problem Statement

「输出是否打转（周期）」「指标是否有惯性（趋势）」「扰动是否
独立（白噪）」缺一把统一的统计尺——逐段肉眼比对不可审计不可
自动化。

## Solution

`Autocorrelation`（core/metrics，纯函数静态件）：

- `lagK(series, k)`：x[0..n−k) 与 x[k..n) 的 Pearson 相关；
- 读法：lag=周期 → ~+1（周期自锁定）/ 半周期 → ~−1（反相）/
  趋势 → 小 lag 高正（惯性）/ 白噪 → 各 lag ~0（无记忆）；
- 零方差 NaN（常数列诚实）；lag≥n / lag<1 / null fail-fast。

## User Stories

1. 作为失控防护作者，输出序列 ACF 高 → 打转嫌疑（周期自锁定）。
2. 作为容量作者，指标惯性可量化——平滑策略选型有据。

## Testing Decisions

- 交替列 lag1 恰 −1 / lag2 恰 +1；趋势 ramp lag1 > 0.95、lag10
  > 0.6；白噪 500 点 lag 1..5 全 |r|<0.15；正弦周期 10 → lag10 恰
  +1、lag5 恰 −1（半周期反相）；常数列 NaN；四路 fail-fast。

## Out of Scope

- 不做 ACF 全谱（各 lag 扫描归调用方循环）；不做偏相关/AR 定阶；
  不做置信带（白噪检验归卡方族）。

## Further Notes

- 与 ChiSquareUniformity（分布判定）/ GrubbsOutlier（单点判定）
  成序列三尺：分布 / 单点 / 结构（相关）。
- 里程碑：38/150。
