# Spec 526 — 水位告警桥接（effort #526）

> wayfinder map：`.wayfinder/maps/effort-526.md`（T805–T806）。E 会话第 27 轮。

## Problem Statement

181 水位是 per-session 事件+全局 gauge——312 告警引擎按机制健康面订阅：
「低水位会话数达阈值」无聚合裁决面。

## Solution

`health.WatermarkHealth implements BuzhouHealth`（mechanism=
context-watermark）+ ContextWatermarkHook 观测访问器（lowWaterSessionCount/
isEnabled）：低水位会话数 ≥ 阈值 → DOWN；hook 未启用=UP（disabled 详情）；
details 聚合读数（lowWaterSessions/alertThreshold/lastUtilization）。

## User Stories

1. 作为运维，我想「3 个会话同时进低水位持续 5 分钟」触发告警， so 容量
   配置过小在压爆前被发现（312 for 窗口防抖）。

## Implementation Decisions

- 未启用=UP（无数据不告警——disabled 归 332 详情）；阈值 ≥1 fail-fast。

## Testing Decisions

- 真实 beforeModel 驱动两会话进低水位 → 阈值 2 时 DOWN；禁用 hook 恒 UP；
  details 读数；阈值 0 fail-fast。

## Out of Scope

- per-session 告警；自动扩容联动。

## Further Notes

- 新公共类型 `WatermarkHealth` 随轮 regenerate 快照 + api-surface.md 加行。
