# Spec 1915 — 百分位排位（effort #1915，R116）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3031–T3032，impl 1516）。借鉴：
> 统计学 percentile rank 惯例——样本集中「≤ v 的占比」：本次成绩
> 超过历史百分之多少。与分位数互补（那是按占比取值，这是按值
> 给排位）。

## Problem Statement

「这次轮次耗时 12s 算慢吗」没有参照系：与历史样本比排位才是
正解——绝对阈值到处失配，相对排位缺独立计算面。

## Solution

`PercentileRank`（core/eval，静态纯函数）：

- `rank(samples, value)`：≤ value 的样本占比（0.0–1.0）——值越
  大排位越高（时延语义：排位越高越差）；
- `percentileOf(samples, value)`：rank×100 取整的百分位读数。

## User Stories

1. 作为评估者，样本 {1,2,3,4,5} 中值 4 → 排位 0.8——超过历史
   80%。
2. 作为报告作者，percentileOf 直读 80 分位——报表口径统一。
3. 作为口径诚实者，低于最小值排位 0.0、高于最大值 1.0——不捏造
   中间值。

## Implementation Decisions

- 纯函数零状态；samples 非空、逐值 ≥ 0 fail-fast；≤ 计数（严格
  小于与等于都算覆盖——时延口径 ≤ 更合理）。

## Testing Decisions

- 排位三例（中位 0.6/最大 1.0/最小 0.2）；百分位直读；畸形两型
  （空表/负值）fail-fast。

## Out of Scope

- 不做流式排位（归 P² 面板）；不做插值细化。

## Further Notes

- 与 PSquareQuantile（流式分位）互补：那是按占比取值，这是按值
  给排位；与 EvalScoreMad 互补：那是离散度，这是相对位置。
