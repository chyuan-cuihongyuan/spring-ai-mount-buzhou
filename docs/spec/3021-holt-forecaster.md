# Spec 3021 — Holt 双参数指数平滑预测器（effort #3021，R22）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5043–T5044，impl 2022）。
> 借鉴：Charles Holt 1957 双参数指数平滑（水平+趋势）。

## Problem Statement

EWMA 只平滑**水平**（无趋势外推能力）——趋势型序列（增长的
成本/延迟爬升/累积用量）的短程预测系统性滞后；线性回归外推又
对窗口敏感、无法在线递推。

## Solution

`HoltForecaster`（core/metrics，单流口径）：

- 双分量递推：level = αx + (1−α)(level+trend)；
  trend = β(level−prevLevel) + (1−β)trend；
- `forecast(h)` = level + h·trend（h=0 即当前水平）；
- 首观测初始化（level=x₀、trend=0）；无观测 NaN 诚实边界；
  α/β∈(0,1)、h≥0 fail-fast。

## User Stories

1. 作为预算作者，累积用量的短程外推——ETA 提前可见。
2. 作为容量作者，延迟爬升趋势可读——恶化速率有数。

## Testing Decisions

- 常量序列水平恒定趋势恰零（α 混合下数学精确）；线性 y=3+2i
  趋势收敛 2±0.05、一步预测贴下一真值 ±0.5；首观测初始化四读；
  forecast(2)−forecast(1) 恒等 trend 且贴斜率 1.5；空态 NaN；
  加速序列（i²）后段趋势高于前中段（趋势跟涨性质）；参数/步数
  五路 fail-fast。

## Out of Scope

- 不做季节性（Holt-Winters 第三分量留白）；不做阻尼趋势（damped
  φ 留白）；不做置信区间；不接 metrics 导出。

## Further Notes

- 与 EwmaEstimator（纯水平）成对：无趋势序列用 EWMA、趋势序列
  用 Holt——选型看趋势显著性。
- 里程碑：22/150。
