# Spec 4038 — Theil-Sen 稳健斜率（effort #4038，R39）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6077–T6078，impl 2139）。
> 借鉴：Theil–Sen 估计（成对斜率中位数；scipy.stats.theilslopes）。

## Problem Statement

趋势估计的病：最小二乘斜率对离群点零抗性（单点杠杆即可
拉偏整条线）、对非正态噪声敏感——**崩溃点 29% 的稳健
趋势面**缺失。

## Solution

`TheilSenSlope`（core/metrics）：

- 斜率 = 全部成对斜率（i<j 且 x_i≠x_j，竖直对跳过）的
  中位数——崩溃点 ≈29%（近三成点污染仍稳）；
- 截距 = median(y_i − slope·x_i)（Sen 截距口径）；
- 中位数偶数取**下中位**（确定性；R27 SpeculativeStraggler
  同款约定）；
- `predict(x)` 便捷读数；纯函数无状态；
- fail-fast：xs/ys 不齐 / n<2 / 竖直退化（无可比对）/
  非有限值。

## User Stories

1. 作为趋势告警作者，离群毛刺不拉偏趋势线——告警不误报。
2. 作为审计作者，同样本同斜率（确定性可回放）。

## Testing Decisions

- 精确线（y=3x+2）斜率截距复原；单点 +1000 污染下 TS 斜率
  稳（±0.5）而 OLS 偏移 >2（对照）；手算四点例（成对斜率
  [-10,0,10,10,10,30] 下中位=10、截距 0）；竖直退化/不齐/
  n<2/非有限 fail-fast。

## Out of Scope

- 不做 O(n log n) 快速算法（O(n²) 小样本口径）；不做置信
  区间（scipy theilslopes alpha 面）；不做加权 TS。

## Further Notes

- 与 LeastSquares 族互补：稳健中位 vs 均值最小化；与
  Grubbs（Q 系离群检验）互补：检验剔除 vs 估计免疫。
  Wave 7 第三件。
- 里程碑：39/50。
