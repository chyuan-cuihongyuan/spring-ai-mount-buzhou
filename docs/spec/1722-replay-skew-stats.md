# Spec 1722 — 回放时钟偏斜读面（effort #1722，R23）（effort #1722，R23）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2645–T2646，impl 1322，impl Kafka consumer lag / NTP 偏斜）。借鉴：回放（重放事件流重建状态）落后多远、有没有时钟倒挂（replayed<original）无读数——回放健康度不可见。

## Problem Statement

`ReplaySkewStats`（core/recovery，实例面 synchronized）：record(originalAt, replayedAt) 逐笔累积；负偏斜（倒挂）单独计数不混入正偏斜统计（诚实分离）；report→SkewReport(samples/medianLag/maxLag/negativeSkewCount 无正样本 −1)。

## Solution

作为恢复运维者，medianLag 大 → 回放管道积压。

## User Stories

1. 17220
2. 17221
3. 17222

## Implementation Decisions

- 17223

## Testing Decisions

- 17224

## Out of Scope

- 17225

## Further Notes

- 17226
