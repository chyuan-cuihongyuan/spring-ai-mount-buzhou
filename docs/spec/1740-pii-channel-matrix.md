# Spec 1740 — PII 通道×类型命中矩阵（effort #1740，R41）（effort #1740，R41）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2681–T2682，impl 1340，impl WAF 命中地图）。借鉴：PII 命中只有总量（PiiHitStats）：哪条通道（输入/流式/导出）漏什么类型无二维分布——脱敏策略盲区不可见。

## Problem Statement

`PiiChannelMatrix`（guard/pii，实例面线程安全）：Channel 三闭集（INPUT/STREAM/EXPORT）×类型键（基数有界 32 超出并 _overflow_）二维计数+record+census 展平降序（unmodifiableMap 保序）+total。纯读面 opt-in。

## Solution

作为脱敏策略者，EXPORT:phone 堆积 → 导出通道漏了手机号脱敏。

## User Stories

1. 17400
2. 17401
3. 17402

## Implementation Decisions

- 17403

## Testing Decisions

- 17404

## Out of Scope

- 17405

## Further Notes

- 17406
