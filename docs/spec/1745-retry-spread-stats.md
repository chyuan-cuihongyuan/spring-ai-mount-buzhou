# Spec 1745 — 重试抖动实效读面（effort #1745，R46）（effort #1745，R46）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2691–T2692，impl 1345，impl AWS Builders' Library / Envoy backoff jitter）。借鉴：jitter 的目的是把重试摊开，但实效无度量：重试延迟相对散布≈0=全撞同一时刻（jitter 失效/没配）。

## Problem Statement

`RetrySpreadStats`（resilience，静态纯函数）：analyze(observedDelaysMillis)→SpreadReport(count/mean/relativeSpread=(max−min)/mean/min/max)；n<2 哨兵 −1；负值忽略；mean=0 记 0（全零延迟无散布可言）。

## Solution

作为重试治理者，散布≈0 → jitter 没配或失效，惊群风险。

## User Stories

1. 17450
2. 17451
3. 17452

## Implementation Decisions

- 17453

## Testing Decisions

- 17454

## Out of Scope

- 17455

## Further Notes

- 17456
