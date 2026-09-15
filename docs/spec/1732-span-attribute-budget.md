# Spec 1732 — span 属性预算审计（effort #1732，R33）（effort #1732，R33）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2665–T2666，impl 1332，impl OpenTelemetry span 属性限额（128 属性/总字节））。借鉴：span 属性数与体积无预算账：超限 span 是管道内存与后端基数的隐形杀手——「哪些在超预算」不可见。

## Problem Statement

`SpanAttributeBudget`（observability，实例面线程安全）：record(spanName, attrCount, attrBytes) 逐 span 记账；可调阈值默认 128 属性/8192 字节；census（spans/overAttrLimit/overByteLimit/worstAttrs/worstBytes）。纯读面 opt-in。

## Solution

作为管道运维者，overByteLimit>0 → 大 span 在撑爆内存limiter。

## User Stories

1. 17320
2. 17321
3. 17322

## Implementation Decisions

- 17323

## Testing Decisions

- 17324

## Out of Scope

- 17325

## Further Notes

- 17326
