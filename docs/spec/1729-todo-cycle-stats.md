# Spec 1729 — todo 返工周期读面（effort #1729，R30）（effort #1729，R30）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2659–T2660，impl 1329，impl Jira reopened issues / GitHub issue reopen）。借鉴：todo 被关闭后又重开 = 返工信号——重开频率与最惨项无读数，模型反复返工同一项不可见。

## Problem Statement

`TodoCycleStats`（tools/todo，实例面线程安全）：recordReopen(itemId) 逐项计数有界默认 128 超出并 _overflow_ 桶（null/空归 _anonymous_）+census→CycleCensus(touchedItems/totalReopens/worstReopens)。与 TodoStalenessAudit（陈旧面）互补。纯读面 opt-in。

## Solution

作为工具治理者，worstReopens=5 → 模型对同一项反复拉锯，查指令清晰度。

## User Stories

1. 17290
2. 17291
3. 17292

## Implementation Decisions

- 17293

## Testing Decisions

- 17294

## Out of Scope

- 17295

## Further Notes

- 17296
