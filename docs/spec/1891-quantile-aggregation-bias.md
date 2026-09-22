# Spec 1891 — 分位数聚合偏差审计（effort #1891，R92）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2983–T2984，impl 1492）。借鉴：
> Prometheus/M3（万星级）惯例——分位数不可加：各分片 p99 的平均
> ≠ 全体 p99（尾部长在哪个分片是概率不是平均数）。naive 聚合的
> 偏差量入账，报告口径有救。

## Problem Statement

多分片观测报告随手平均：「两分片 p99 各 100ms/500ms，平均 p99
300ms」——真实全体 p99 可能是 500ms（偏差 −40%）；SLA 报告若按
naive 口径出，违约判定全错。偏差有没有、多大，缺独立审计面。

## Solution

`QuantileAggregationBias`（core/metrics，静态纯函数）：

- `naiveAverage(shardQuantiles)`：分位平均——naive 聚合读数（即
  常见错误口径本身，可对照）；
- `biasRatio(naive, actual)`：(naive − actual)/actual——符号化偏差
  （负 = naive 低报，低报最危险）；
- `isMateriallyBiased(naive, actual, tolerance)`：|偏差比| ≥ 容差
  → naive 口径不可用。

## User Stories

1. 作为 SLO 报告者，分片 {100,500} naive 300 vs 真值 500 → 偏差
   −40%——报告必须改口径。
2. 作为阈值裁定者，容差 10% 内可豁免——偏差审计可配置。
3. 作为演示者，naiveAverage 公开导出——「错误答案长什么样」可示众。

## Implementation Decisions

- 纯函数零状态；分片值非空且 ≥ 0（时延量纲）fail-fast；actual > 0
  fail-fast（真值 0 无偏差可谈）；对称偏差取绝对值判定。

## Testing Decisions

- 经典 {100,500} naive=300；真值 500 → biasRatio −0.4；±容差两
  侧行为；畸形三型（空表/负值/actual=0）fail-fast。

## Out of Scope

- 不做真实分位计算（归直方/分位素描面）；不定位长尾分片。

## Further Notes

- 与 LogBucketHistogram/PSquareQuantile 互补：那是单流分位，这是
  跨流聚合的口径审计。
