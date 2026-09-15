# Spec 1725 — 检索命中排名读面（effort #1725，R26）（effort #1725，R26）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2651–T2652，impl 1325，impl Elasticsearch rank_eval / RecSys MRR）。借鉴：检索器返回排序列表，但最终被使用的条目排第几无读数——前排全是噪声还是正中红心，排名质量不可见。

## Problem Statement

`RetrievalRankStats`（memory/recall，实例面线程安全）：record(rank)（1 基；<=0 miss）+report→RankReport(hits/misses/mrr 平均倒数排名无命中 −1/hitAt1/hitAt3)。纯读面 opt-in，不改 MultiQueryRetriever/RecallSearch。

## Solution

作为检索调参者，MRR 0.9 → 排序器优秀；0.4 → 前排噪声多。

## User Stories

1. 17250
2. 17251
3. 17252

## Implementation Decisions

- 17253

## Testing Decisions

- 17254

## Out of Scope

- 17255

## Further Notes

- 17256
