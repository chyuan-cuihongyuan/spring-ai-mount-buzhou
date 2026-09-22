# Spec 1902 — 波动系数读面（effort #1902，R103）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3005–T3006，impl 1503）。借鉴：
> 金融统计 CV（coefficient of variation）语义——σ/μ 无量纲化：
> 绝对波动不可比（百元股 ±10 与十元股 ±10 是两回事），相对波动
> 跨量纲可比并分档。

## Problem Statement

延迟/成本波动的稳定性评估两眼一抹黑：stddev 10ms 在均值 50ms 下
剧烈、在均值 5s 下平静——绝对标准差跨规模不可比，缺无量纲读面
与分档语义。

## Solution

`CoefficientOfVariation`（core/metrics，静态纯函数 + Volatility
枚举）：

- `cv(mean, stddev)`：stddev/|mean|——无量纲波动系数（mean=0 未
  定义 fail-fast——分母为零的 CV 无意义）；
- `band(cv)`：三档分位——<0.15 STABLE / <0.5 MODERATE / 其余
  VOLATILE（金融惯例带）。

## User Stories

1. 作为稳定性评审者，mean 100 stddev 10 → CV 0.1 STABLE；mean 50
   stddev 25 → CV 0.5 VOLATILE——同 stddev 两重天可量化。
2. 作为告警作者，band 分档接健康面——VOLATILE 即抖动信号。
3. 作为跨团队对比者，无量纲读数让延迟/成本/吞吐同一把尺。

## Implementation Decisions

- 纯函数零状态；stddev ≥ 0、mean ≠ 0 fail-fast（分母零未定义不
  哑算）；分档边界 < 严格小于（0.15/0.5 各自不含）。

## Testing Decisions

- 三档各一例（0.1/0.3/0.5）；负均值取绝对值；mean=0 与负 stddev
  fail-fast 两例。

## Out of Scope

- 不做均值方差累计（归 WelfordAccumulator 面）；不做分位数波动。

## Further Notes

- 与 WelfordAccumulator（单遍均值方差）互补：那是累计器，这是
  派生读数；与 EvalScoreMad（绝对偏差中位）互补：那是鲁棒离散，
  这是相对离散。
