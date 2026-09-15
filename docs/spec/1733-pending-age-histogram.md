# Spec 1733 — 待决事件年龄直方（effort #1733，R34）（effort #1733，R34）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2667–T2668，impl 1333，impl Kafka consumer lag exporter）。借鉴：观测管道待决事件（已入队未落库）的年龄无分布——实时还是积压、最老积压多久，lag 分桶缺位。

## Problem Statement

`PendingAgeHistogram`（observability，实例面线程安全桶式房规）：默认边界 1s/10s/1m/5m → 5 桶（<1s/<10s/<1m/<5m/更老）+record 负值忽略+oldestMillis 哨戒。与 PendingSnapshot（瞬时清单）互补。纯读面 opt-in。

## Solution

作为管道运维者，更老桶堆积 → 消费侧跟不上了。

## User Stories

1. 17330
2. 17331
3. 17332

## Implementation Decisions

- 17333

## Testing Decisions

- 17334

## Out of Scope

- 17335

## Further Notes

- 17336
