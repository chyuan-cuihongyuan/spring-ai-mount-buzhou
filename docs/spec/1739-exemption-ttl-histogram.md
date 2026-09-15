# Spec 1739 — 豁免 TTL 直方（effort #1739，R40）（effort #1739，R40）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2679–T2680，impl 1339，impl cert-manager 证书生命周期普查）。借鉴：GuardExemptionRegistry 的豁免 TTL 分布不可见：短票还是长期通行证——长期豁免堆积=权限漂移温床。

## Problem Statement

`ExemptionTtlHistogram`（guard，实例面线程安全桶式房规）：默认 1m/10m/1h/24h 五桶+permanent 独立计数（0 永久不入桶）。纯读面 opt-in。

## Solution

作为权限审计者，permanent 堆积 → 豁免变终身制，权限漂移告警。

## User Stories

1. 17390
2. 17391
3. 17392

## Implementation Decisions

- 17393

## Testing Decisions

- 17394

## Out of Scope

- 17395

## Further Notes

- 17396
