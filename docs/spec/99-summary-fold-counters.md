# Spec 99 — 折入速率指标（effort #61）

> wayfinder map：`.wayfinder/maps/effort-61.md`（T369–T370）。spec 95 fog 项收口。

## Problem Statement

摘要折入的 trigger 溯源进了事件（spec 95），但指标面缺席：漂移/积压/预算触发率
趋势、breaker 开路导致的「该折未折」堆积量不可聚合告警。

## Solution

IVP 两个 counter（BuzhouMetricsHolder，tag trigger ∈ budget/backlog/drift 有界）：
- `buzhou.memory.summary.folded`——折入成功点（与 onSummaryFolded 通知同点同值）；
- `buzhou.memory.summary.fold-skipped`——breaker 开路跳过（独立分支采集，带
  trigger 标注堆积来源）。

## User Stories

1. 作为运维，我要触发率趋势可聚合，所以压缩策略调优有数据。
2. 作为告警作者，我要跳过量可观测，所以摘要熔断开路不静默积压。

## Implementation Decisions

- 跳过在 `allows()==false` 分支独立计数（不引入 folded 的反例语义）。

## Testing Decisions

- 成功折入 → folded:trigger=backlog 恰一次；烧断 breaker → fold-skipped 且无
  folded 误报。

## Out of Scope

- 折入时延 timer；payload 扩展。

## Further Notes

- 与 spec 83/84 counter 家族同纪律（tag 有界枚举）。
