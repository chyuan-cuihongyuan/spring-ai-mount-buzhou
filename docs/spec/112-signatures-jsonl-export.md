# Spec 112 — 错误签名 JSONL 导出（effort #74）

> wayfinder map：`.wayfinder/maps/effort-74.md`（T407–T408）。spec 83 fog 项收口。

## Problem Statement

错误签名（spec 83）只在进程内：top()/健康段截 5 条——各族随时间的消长趋势
（错误治理效果的量化）需要全量导出进 OLAP。

## Solution

`ErrorSignaturesJsonl.export(registry, Writer)`（静态面）：snapshot() 全量一行一
JSON（`{"signature":...,"count":N}`，count 降序与 top 同序）；Jackson 转义纪律；
空表零行；返回行数。导出五族补齐：观测 span/event（spec 60）、eval run（88）、
ab run（94）、错误签名（本 spec）。

## User Stories

1. 作为 SRE，我要错误族趋势进数仓，所以治理效果（某族清零）可量化。
2. 作为红队，我要空表诚实零行，所以导出不产生噪音行。

## Implementation Decisions

- 无时间戳列（进程内表无时间维——OLAP 侧以导出时刻落时间列）。

## Testing Decisions

- 三族（3/1/1）→ 三行 count 降序 + 逐行独立 JSON；空表零行零字节。

## Out of Scope

- 导出后清零（窗口化）；gzip（spec 109 面另挂）。

## Further Notes

- 运维组合：cron exportAll 式定时导出 + DuckDB 趋势 SQL——错误治理闭环。
