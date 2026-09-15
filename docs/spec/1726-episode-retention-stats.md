# Spec 1726 — 情节保留普查（effort #1726，R27）（effort #1726，R27）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2653–T2654，impl 1326，impl Kafka log segment retention）。借鉴：EpisodeLedger 的情节被什么赶走（TTL 到期 vs 容量挤占）无分布——保留策略调参方向不明。

## Problem Statement

`EpisodeRetentionStats`（memory/episodic，实例面线程安全）：RetentionEvent 三闭集（STORED/EVICTED_TTL/EVICTED_CAPACITY）+record(event,n)（批量逐出一次记多条，n<0 忽略）+census+evictRatio（无样本 −1）+resetForTest。纯读面 opt-in。

## Solution

作为保留调参者，TTL 逐出为主 → 调 TTL；容量为主 → 调容量上限。

## User Stories

1. 17260
2. 17261
3. 17262

## Implementation Decisions

- 17263

## Testing Decisions

- 17264

## Out of Scope

- 17265

## Further Notes

- 17266
