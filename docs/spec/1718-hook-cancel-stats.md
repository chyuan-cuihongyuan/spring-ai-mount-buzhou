# Spec 1718 — 钩子取消面统计（effort #1718，R19）（effort #1718，R19）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2637–T2638，impl 1318，impl OpenTelemetry exporter 取消路径遥测）。借鉴：HookChain 在轮次取消时短路跳过未执行钩子是无声的——「没跑是因为取消」还是「没注册」无从分辨，取消占比不可见。

## Problem Statement

`HookCancelStats`（core/hook，实例面线程安全）：record(boolean ran)（false=因取消跳过）+snapshot→CancelSnapshot(observed/cancelledSkipped/completed/cancelRatio 无样本 −1)+resetForTest。纯读面 opt-in。

## Solution

作为排查者，cancelRatio 高 → 钩子缺失是取消所致，非装配缺失。

## User Stories

1. 17180
2. 17181
3. 17182

## Implementation Decisions

- 17183

## Testing Decisions

- 17184

## Out of Scope

- 17185

## Further Notes

- 17186
