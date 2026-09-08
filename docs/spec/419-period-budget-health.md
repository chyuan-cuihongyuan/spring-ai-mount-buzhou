# Spec 419 — 周期预算健康面（effort #419）

> wayfinder map：`.wayfinder/maps/effort-419.md`（T729–T730）。D 会话第 20 轮。

## Problem Statement

周期预算（408）只有事件无健康面：本周期消耗进度（烧到哪了）与回血
时刻（还有多久重置）不可见——运维最常问的两问无数据面。

## Solution

`core.health.PeriodBudgetHealth`（机制名 `period-budget`；VirtualKeysHealth
154 先例——**恒 UP 观测面**：耗尽由预算闸拦截，健康面只报事实不裁决）：

- **details**：{unit, period(tag), tokens, tokensLimit, tokensPct,
  costMicroUsd, costMicroUsdLimit, costPct, exhaustedTokens,
  exhaustedCost, **resetsAt**（下周期起点 ISO——月=下月 1 日 00:00、
  周=下 epochWeek 界、日=次日 00:00；Clock 注入测试确定性）}。
- pct = min(100, used*100/limit)；limit 缺省（null 轨）对应字段 null。
- 无 hook（未启用预算周期）UNKNOWN。
- 装配：与 `buzhou.budget.period.enabled` 同键属性条件（312 注记口径）。

## User Stories

1. 作为运维，我想看本周期消耗进度与百分比，so 「还剩多少额度」一屏可答。
2. 作为运维，我想看回血时刻，so 被拦后知道等多久（或该去调预算）。
3. 作为 SRE，我想该面恒 UP，so 预算拦截不被误判为故障触发重启。

## Implementation Decisions

- 只读快照（读 PeriodBudgetHook 累计——不推进状态）。

## Testing Decisions

- details 数值（双轨+pct 截断）；exhausted 布尔；resetsAt 三粒度各自
  正确（Clock 拨动）；未启用 UNKNOWN；装配同键出现/缺席。

## Out of Scope

- 告警（恒 UP 不触发——348 同注记）；DEGRADED 裁决。

## Further Notes

- 新公共类型 `PeriodBudgetHealth` 随轮 regenerate 快照。
