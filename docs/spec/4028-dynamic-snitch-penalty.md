# Spec 4028 — 动态 snitch 惩罚（effort #4028，R29）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6057–T6058，impl 2129）。
> 借鉴：Cassandra DynamicEndpointSnitch badness。

## Problem Statement

副本池的病副本（延迟劣化但未死）持续吃流量——**持续观测的
动态降权**面缺失（静态偏好不追故障）。

## Solution

`DynamicSnitchPenalty`（core/policy）：

- 每副本 EWMA 平滑延迟（α=0.5——抖动不惊弓）；
- 显著慢于最快副本 ×threshold（>1）者加固定罚分——ranking
  把病副本推队尾（读修复/一致性读优先健康副本）；
- 恢复后 EWMA 回落自动免罚复用；未见副本零知识不罚（NaN
  诚实，不入 ranking）。

## User Stories

1. 作为读路径作者，病副本自动降权——流量追健康。
2. 作为运维作者，恢复自动复用——不手工摘副本。

## Testing Decisions

- 50>10×1.5 罚（score 150）与 10 免罚双证 + ranking 推尾；
  EWMA 单调收敛五步向新观测；60 罚后连续 4ms×10 回落免罚；
  未见副本 NaN/不罚/不入 ranking；畸形五型 fail-fast。

## Out of Scope

- 不做真探测调度（归健康检查）；不做动态罚分递增
 （固定罚分口径）；不做跨数据中心权重。

## Further Notes

- 与 TwoChoiceSelector（单次两问取轻）互补：持续观测动态偏好
  vs 单次选择。Wave 5（调度与放置族）收口。
- 里程碑：29/50。
