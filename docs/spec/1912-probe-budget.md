# Spec 1912 — 探测流量预算（effort #1912，R113）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3025–T3026，impl 1513）。借鉴：
> SRE 健康检查预算惯例——探测本身也是流量：N 实例 × 探测频率的
> QPS 占总容量比超限即「探测反噬」（探活变压死），占比读数与上限
> 判定前置。

## Problem Statement

健康探测频率拍脑袋：实例数上涨后探测 QPS 随实例数线性膨胀——
探测把服务打挂（探活变压死）的事故反复发生；探测流量占比没有
独立预算判定面。

## Solution

`ProbeBudget`（core/health，静态纯函数 + Verdict 枚举）：

- `probeShare(probeQps, capacityQps)`：探测流量占比读数；
- `verdict(share, maxShare)`：≤ maxShare → WITHIN（预算内）；
  > maxShare → OVER（超预算，降频或扩容）。

## User Stories

1. 作为 SRE，探测 50 QPS/容量 1000 QPS → 5%——预算 10% 内安全。
2. 作为扩容触发者，实例翻倍后占比 12% → OVER——先降频或扩容。
3. 作为评审者，占比读数让「探测反噬」事前可见。

## Implementation Decisions

- 纯函数零状态；probeQps ≥ 0、capacityQps ≥ 1、maxShare ∈ (0,1]
  fail-fast。

## Testing Decisions

- 占比读数两例（0.05/0.12）；判定两态（含上边界恰 maxShare =
  WITHIN）；畸形三型 fail-fast。

## Out of Scope

- 不做真实探测执行（归健康面）；不做自适应降频。

## Further Notes

- 与 TTL 探针状态机（R14）互补：那是探测状态机，这是探测流量的
  总量预算。
