# Wayfinder Map — Buzhou 周期预算健康面（effort #419，D 会话第 20 轮）

> D 会话第 20 轮（#408 扩散轮）。勘察：#408 PeriodBudgetHook 只有事件——
> 周期消耗进度无健康面；VirtualKeysHealth 先例（恒 UP 观测面：配额耗尽
> 由预算闸拦截，健康面只报事实不裁决）。运维最常问的「本周期烧到哪了/
> 还有多久回血」不可见。

## Destination

`core.health.PeriodBudgetHealth`（机制名 period-budget，恒 UP——154 先例
口径）：details = {unit, period, tokens/limit/pct, costMicroUsd/limit/pct,
exhaustedTokens/Cost 布尔, resetsAt（**下周期起点**——月=下月 1 日、周=
下 epochWeek 界、日=次日；Clock 注入）}；无 hook（未启用）UNKNOWN。
装配与 buzhou.budget.period.enabled 同键（属性条件——312 注记口径）。

## Notes

- 号段：spec 419 / T729–T730 / impl-392。
- 借鉴源：VirtualKeysHealth（154——恒 UP 观测面）+ AWS Budgets 面板
  （进度+重置时刻）。
- 纪律：只读快照（不推进状态）；pct 上限 100（>限截断显示）。

## Out of scope

- 告警（312 族消费 details 可配 for 规则但恒 UP 不触发——348 同注记）；
  DEGRADED 裁决（Status 枚举无此值——诚实边界）。

## Tickets

- [x] [T729 PeriodBudgetHealth](../tickets/T729-period-budget-health.md)
- [x] [T730 resetsAt 计算 + 装配](../tickets/T730-period-health-assembly.md)
