# Spec 1723 — 悬挂修复动作结果普查（effort #1723，R24）（effort #1723，R24）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2647–T2648，impl 1323，impl Kubernetes events 自愈动作审计）。借鉴：DanglingCallRepairer 修悬挂调用，但修复动作做了什么选择（重放/标记失败/目标已消失跳过）无普查——自愈动作分布不可见。

## Problem Statement

`RepairOutcomeStats`（core/recovery，实例面线程安全）：Action 闭集（REPLAYED/MARKED_FAILED/SKIPPED_GONE）+record+census→RepairCensus(total/replayed/markedFailed/skippedGone/replayRatio 无样本 −1)+resetForTest。纯读面 opt-in。

## Solution

作为恢复运维者，REPLAYED 占比高 → 自愈真在干活。

## User Stories

1. 17230
2. 17231
3. 17232

## Implementation Decisions

- 17233

## Testing Decisions

- 17234

## Out of Scope

- 17235

## Further Notes

- 17236
