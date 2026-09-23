# Spec 4037 — Holt-Winters 季节指数（effort #4037，R38）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6075–T6076，impl 2138）。
> 借鉴：Holt-Winters 三参数加法季节模型（statsmodels ExponentialSmoothing）。

## Problem Statement

周期性负载预测的病：无季节项的水平+趋势外推（HoltForecaster）
在周期信号上系统性高估/低估（预测永远追不上节律）——
**季节指数在线学习面**缺失。

## Solution

`HoltWintersIndex`（core/metrics，加法口径）：

- 三参数在线更新：水平 `L=α(y−S_{t−m})+(1−α)(L+b)`、
  趋势 `b=β(L−L′)+(1−β)b`、季节 `S_t=γ(y−L)+(1−γ)S_{t−m}`；
- 预热启发式（statsmodels 同款）：首季均值定水平、次季均值
  差定趋势、首季去均值定季节初值（需恰 2 季预热）；
- `forecast(h)`：`L + h·b + S[(n+h) mod m]`；
- 读数面：level()/trend()/seasonalIndexOf(slot)/warmedUp()；
- fail-fast：period<2、α/β/γ∉(0,1)、非有限观测；预热未满
  forecast/level ISE（未毕业诚实不猜）。

## User Stories

1. 作为容量规划作者，周期负载（日/周节律）被季节项吸收——
   预测追上节律。
2. 作为审计作者，同观测序列同预测轨迹（确定性可回放）。

## Testing Decisions

- 线性+季节信号五季学习后逐位预测（容差内）；常值序列
  季节指数收敛近零 + 水平收敛真值；seasonalIndexOf 读数；
  确定性回放；参数越界/period<2/非有限观测 fail-fast +
  预热未毕业 ISE。

## Out of Scope

- 不做乘法口径与阻尼趋势（damped）；不做网格搜参/优化
 （α/β/γ 定构给定）；不做多季并行（单序列口径）。

## Further Notes

- 与 HoltForecaster 同族不同面：水平+趋势 vs 水平+趋势+
  季节指数。Wave 7 第二件。
- 里程碑：38/50。
