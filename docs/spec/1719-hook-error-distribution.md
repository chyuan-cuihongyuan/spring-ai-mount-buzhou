# Spec 1719 — 钩子异常类型分布（effort #1719，R20）（effort #1719，R20）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2639–T2640，impl 1319，impl Sentry 事件分组）。借鉴：钩子抛异常只有日志：哪个钩子在频繁炸什么异常、炸了多少次——指纹分组计数缺位，坏钩子淹没在日志里。

## Problem Statement

`HookErrorDistribution`（core/hook，实例面线程安全）：record(hookName, throwable) 按「钩子名:异常简单类名」指纹分组计数；分组基数有界默认 32 超出并 _overflow_ 桶（基数纪律）；census() 降序保序（unmodifiableMap）+total()。

## Solution

作为平台运维，auth:IllegalStateException 占大头 → 修一个点。

## User Stories

1. 17190
2. 17191
3. 17192

## Implementation Decisions

- 17193

## Testing Decisions

- 17194

## Out of Scope

- 17195

## Further Notes

- 17196
