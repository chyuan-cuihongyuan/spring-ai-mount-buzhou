# Spec 4022 — 尾采样策略（effort #4022，R23）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6045–T6046，impl 2123）。
> 借鉴：OpenTelemetry tail-based sampling；Jaeger adaptive 同族。

## Problem Statement

头采样在 span 起点盲抽——错误/慢轨迹与正常轨迹同概率被丢，
**排障金料恰最易丢**；整条轨迹完成后再判的件缺失。

## Solution

`TailSamplingPolicy`（core/observability）：

- 错误必采（金料通道不受预算影响）；慢必采（时延 ≥ 阈值**含等**）；
- 其余概率基线（随机源可注入回放）；概率分支受**采样预算**封顶
 （预算尽 → budget 丢弃——观测成本有硬顶而金料不丢）；
- Verdict(sampled, reason) 四理由（error/slow/probabilistic/budget/
  below-threshold）+ 四计数守恒账面。

## User Stories

1. 作为排障作者，错误与慢轨迹全保——金料不丢。
2. 作为成本作者，概率通道有预算硬顶——观测费不失控。

## Testing Decisions

- 概率 0+预算 0 也不拦错误；999 阈下丢/1000 恰阈采含等/5000 采；
  p=1 全采、p=0 全丢、p=0.5 千次 400–600（种子确定）；预算 2 恰
  采两后 budget 丢 + 错误/慢通道不受影响；畸形六型 fail-fast。

## Out of Scope

- 不做头采样一致性传播（已有件覆盖）；不做多策略组合/延迟决策
  窗；不做 per-service 差异化（单策略口径）。

## Further Notes

- 与头采样成对：金料全保+成本可控双档。Wave 4（队列与流控族）
  收口。
- 里程碑：23/50。
