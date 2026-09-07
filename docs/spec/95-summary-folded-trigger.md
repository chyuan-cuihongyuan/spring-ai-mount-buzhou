# Spec 95 — 摘要折入 trigger 溯源（effort #56）

> wayfinder map：`.wayfinder/maps/effort-56.md`（T359–T360）。spec 90 fog 项收口。

## Problem Statement

摘要折入（预算/积压/漂移三判据，spec 70/90）在观测面缺席：memory.compacted 只有
微压缩路径事件，摘要折入零事件——「这个会话的上下文为何变短了」在 OLAP 里查不到
判据来源。

## Solution

`CompactionListener` 新 default 方法 `onSummaryFolded(sessionId, summary, trigger)`：
- trigger ∈ `budget`（预算压）/ `backlog`（积压阈值）/ `drift`（语义漂移）；
  判定顺序 budget 优先（硬性判据）→ drift → backlog；
- default 空实现——既有 lambda 监听器零改动（二进制兼容）；
- IVP 在 `summaryBridge.save` 成功后回调（try-catch lenient，同 notifyCompaction
  纪律——观测双写失败不影响视图主链）；
- MemoryModule 装配升级匿名类：双写 `memory.summary.folded` 事件（payload:
  trigger / generation / coversUpToTurn）。

## User Stories

1. 作为运维，我要折入事件带判据来源，所以漂移触发率与积压触发率可分别观测。
2. 作为监听器作者，我要既有 lambda 不破，所以升级零成本。

## Implementation Decisions

- default 方法而非新接口（CompactionListener 家族聚合——微压缩与摘要折入是同一
  观测域）。
- trigger 判定在折入成功点现算（不透传状态——避免参数膨胀）。

## Testing Decisions

- 积压触发 → trigger=backlog + coversUpToTurn 推进；
- 漂移触发（backlog=0 排除干扰）→ trigger=drift；
- memory 全量回归（装配匿名类路径）。

## Out of Scope

- 折入速率 counter；breaker 开路失败事件；payload 扩展。

## Further Notes

- memory.summary.folded 与 memory.compacted 构成压缩观测双事件族。
