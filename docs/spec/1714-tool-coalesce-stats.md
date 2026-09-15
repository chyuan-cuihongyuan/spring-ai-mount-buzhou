# Spec 1714 — 工具合并节省读面（effort #1714，R15）（effort #1714，R15）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2629–T2630，impl 1314，impl Go singleflight / groupcache 合并回喂遥测）。借鉴：ToolCallCoalescer 把同参并发调用合并成一次真实执行，但省了多少从不可见——合并收益（组数/省去调用/省去时延）无账，功能形同黑盒。

## Problem Statement

`ToolCoalesceStats`（core/exec，实例面线程安全）：recordGroup(members)（members≥2 记账，省去 members−1 次）+recordLatencySaved(millis)（负值忽略）+snapshot()→CoalesceSavings(groups/callsJoined/savedCalls/latencySavedMillis/savingRatio 无合并哨兵 −1)+resetForTest。

## Solution

作为性能调参者，savingRatio=0.2 → 五分之一调用被省，缓存命中率替代指标。

## User Stories

1. 17140
2. 17141
3. 17142

## Implementation Decisions

- 17143

## Testing Decisions

- 17144

## Out of Scope

- 17145

## Further Notes

- 17146
