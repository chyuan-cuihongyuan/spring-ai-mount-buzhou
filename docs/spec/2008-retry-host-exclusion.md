# Spec 2008 — 重试主机排除（effort #2008，R9）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3117–T3118，impl 1559）。
> 借鉴：Envoy retry host predicate——重试不落同一坏端点。

## Problem Statement

备模型回退链重试时，默认可能再次命中刚失败的那个端点——坏端点被
反复敲打：重试放大故障端流量，恢复延迟；且无「哪些端点正在让位」
的显形读数。

## Solution

`RetryHostExclusion`（buzhou-resilience routing，synchronized 小临界区）：

- `recordFailure(host, now)`：失败记冷却起点（再失败重算——冷却顺延）；
- `filterCandidates(candidates, now)`：剔除冷却窗（默认 30s）内失败过
  的 host，**候选序保持**（路由权重序不动）；候选全被排除时**回退
  全量**（Envoy 语义：排除是偏好不是硬门——不制造空候选空转）；
- `excludedCount(candidates, now)`：冷却中（将被排除）的候选数——
  全排除回退态显形对账面；
- 契约：cooldownMillis > 0、host/candidates 非空、now ≥ 0 fail-fast。

## User Stories

1. 作为回退链作者，attempt-2 不落 attempt-1 的失败端点——重试换道。
2. 作为可用性守卫，全排除时回退全量——唯一端点冷却中也仍可一试。
3. 作为观测者，excludedCount==候选数 即「全排除回退态」显形。

## Implementation Decisions

- 时间由调用方传入（确定性可回放）；冷却恰到期即回归（界内才排除）。

## Testing Decisions

- 失败者让位；到期回归（边界恰 1000ms）；全排除回退全量 + 显形计数；
  多失败累积（不同冷却起点独立回归）；序保持；再失败冷却顺延；畸形
  五型 fail-fast。

## Out of Scope

- 不接 ResilienceAdvisor 回退链（接线归后续轮）；
- 不做 attempt 序号精确对位（Envoy predicate 按次排除的强语义留白）。

## Further Notes

- 与 OutlierEjection（熔断驱逐）互补：驱逐是长时统计排除，本件是
  短窗重试让位。
