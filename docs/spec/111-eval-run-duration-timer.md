# Spec 111 — 评估 run 时长 timer（effort #73）

> wayfinder map：`.wayfinder/maps/effort-73.md`（T405–T406）。LangSmith run latency 借鉴。

## Problem Statement

评估面时长只有 per-item durationMs（落盘明细，查询才可见）：整跑时长（数据集
规模感知——同一数据集时长回归即规模/模型劣化信号；评估风暴检测）无指标维。

## Solution

双 timer（BuzhouMetricsHolder，完成点计时）：
- `buzhou.eval.run.duration`——EvalRunner 完成点（复用既有 startedAt→finishedAt）；
- `buzhou.eval.ab-run.duration`——PairwiseEvalRunner 完成点。
执行/落盘/事件零变化（异常已收敛为 item error——整跑恒达完成点）。

## User Stories

1. 作为运维，我要整跑时长趋势，所以数据集/模型劣化在 CI 即可见。
2. 作为告警作者，我要 ab-run 时长，所以对比风暴（双倍调用）可检测。

## Testing Decisions

- 一次 eval + 一次 ab → 双 timer 各恰一次（过滤 turn.duration 噪声）+ 纳秒非负。

## Out of Scope

- per-dataset 维度（无界 tag）；分位数告警阈值。

## Further Notes

- 与 spec 77（在飞 gauge）、108（工具 timer）组成评估三维修测：在飞/整跑/项内。
