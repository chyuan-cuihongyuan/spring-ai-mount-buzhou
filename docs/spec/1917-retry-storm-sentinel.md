# Spec 1917 — 重试风暴哨兵（effort #1917，R118）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3035–T3036，impl 1518）。借鉴：
> SRE 重试风暴惯例——故障期重试流量占比飙升至一半以上即「风暴」：
> 每个失败触发重试、重试又失败（放大系数 Σpⁿ），占比读数 + 阈值
> 判定让风暴可测可警。

## Problem Statement

上游故障期流量放大没人看见：重试占比从 5% 飙到 60% 的过程无刻度
——等限流器报错时风暴已经成形；「重试占比」作为独立的哨兵信号
缺判定面。

## Solution

`RetryStormSentinel`（core/backpressure，静态纯函数）：

- `retryRatio(totalRequests, retriedRequests)`：重试占比读数；
- `isStorm(ratio, threshold)`：占比 ≥ 阈值即风暴（边界含上）。

## User Stories

1. 作为哨兵作者，100 请求 60 重试 → 占比 0.6 ≥ 0.5 → 风暴告警。
2. 作为故障复盘者，占比时间序列回看——风暴从哪一秒开始可查。
3. 作为口径诚实者，总请求 0 哨兵 fail-fast（无流量不判风暴）。

## Implementation Decisions

- 纯函数零状态；total ≥ 1、0 ≤ retried ≤ total、阈值 ∈ (0,1]
  fail-fast。

## Testing Decisions

- 占比两例（0.3/0.6）；判定两侧恰阈值含上；畸形三型（零总量/
  重试超总量/阈值越界）fail-fast。

## Out of Scope

- 不做重试计数维护（归 RetryBudget 面）；不做自动熔断（归熔断器）。

## Further Notes

- 与 RetryBudget 互补：那是预算余额（事前限制），这是占比哨兵
  （事后告警）。
