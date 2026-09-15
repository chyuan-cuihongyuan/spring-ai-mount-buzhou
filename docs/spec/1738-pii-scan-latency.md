# Spec 1738 — PII 扫描耗时分位读面（effort #1738，R39）（effort #1738，R39）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2677–T2678，impl 1338，impl Envoy per-filter 计时）。借鉴：PII 扫描在每个请求路径上跑，扫描自身吃掉多少延迟无账——安全不能比漏洞更慢，扫描延迟分布缺位。

## Problem Statement

`PiiScanLatency`（guard/pii，实例面线程安全）：record(scanMillis)（负值忽略）累积+report（samples/median/p95 最近秩/max，无样本 −1）。与 PiiHitStats（命中面）互补。纯读面 opt-in。

## Solution

作为安全运维者，p95 扫描 50ms → 每请求多 50ms 尾延迟，先优化扫描再谈 SLA。

## User Stories

1. 17380
2. 17381
3. 17382

## Implementation Decisions

- 17383

## Testing Decisions

- 17384

## Out of Scope

- 17385

## Further Notes

- 17386
