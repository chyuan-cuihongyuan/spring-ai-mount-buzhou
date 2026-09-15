# Spec 1744 — 对冲请求节省读面（effort #1744，R45）（effort #1744，R45）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2689–T2690，impl 1344，impl Google「The Tail at Scale」/ Envoy request hedging）。借鉴：HedgedChatModel 发对冲请求，但赢率与节省无账：对冲赢率低=白花钱，赢率高=阈值可更激进。

## Problem Statement

`HedgeStats`（resilience/fallback，实例面线程安全）：recordHedge/recordPrimaryWin/recordHedgeWin/recordLatencySaved（负值忽略）四计数+census（hedgeWinRatio 无决胜 −1）。纯读面 opt-in。

## Solution

作为成本治理者，赢率 0.3 → 七成对冲白发，调高触发阈值。

## User Stories

1. 17440
2. 17441
3. 17442

## Implementation Decisions

- 17443

## Testing Decisions

- 17444

## Out of Scope

- 17445

## Further Notes

- 17446
