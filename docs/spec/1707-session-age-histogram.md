# Spec 1707 — 会话年龄分桶直方（effort #1707，R8）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2615–T2616，impl 1307）。借鉴：
> Prometheus histogram；Redis INFO 的键年龄分布直觉——「活多久」与「闲多久」
> 是两个治理轴。

## Problem Statement

IdleDurationHistogram 量空闲，但会话**存活年龄**（自创建至今）无分桶：
清理策略（RetentionSweeper/SessionCleaner）调参没有「库里到底多少老古董」
的依据，长龄会话的内存驻留风险不可见。

## Solution

`SessionAgeHistogram`（core/session，实例面线程安全，IdleDurationHistogram
桶式房规镜像）：默认边界 1h/1d/7d → 4 桶（fresh/当日/当周/更老）；
`record(ageMillis)`（负值忽略）+ `bucketCounts()` + `total()` +
`eldestMillis()`（最老年龄哨戒）。自定义升序边界 n→n+1 桶。

## User Stories

1. 作为清理策略调参者，「更老」桶 40% → 收紧 retention 或先归档。
2. 作为运维，eldestMillis=21 天 → 哨戒最老活会话。

## Implementation Decisions

- AtomicLongArray 线程安全（与 Idle 同款）；桶互斥按升序边界。
- 纯读面 opt-in：不改任何清理行为。

## Testing Decisions

- 默认桶互斥四带各一例；负值忽略；自定义边界 n+1 桶；eldest 哨戒。

## Out of Scope

- 不做自动清理联动/不做时间窗自滚动（调用方喂当前年龄）。

## Further Notes

- 年龄（本轮）+ 空闲（既有）+ 轮间节奏（spec 1711）= 会话时间三维读面。
