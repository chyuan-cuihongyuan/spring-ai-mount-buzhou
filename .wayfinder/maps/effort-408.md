# Wayfinder Map — Buzhou 预算日历周期（effort #408，D 会话第 9 轮）

> D 会话第 9 轮。勘察（2026-09-08）：预算面三态——会话级硬顶
> （TokenBudgetHook）+ 虚拟 key 预算（VirtualKeys，总量无翻页）+ 日配额
> （SessionQuotaHook epochDay 隐式翻页）。**月度/周度日历周期预算缺失**：
> 「这个月全进程最多花 100M tokens」这种最常见的账期语义没有承载。

## Destination

`core.budget.PeriodBudgetHook`（AWS Budgets calendar period 借鉴）：全进程
日历周期预算——unit=MONTHLY/WEEKLY/DAILY，periodTag（yyyy-MM /
epochWeek / epochDay）入键，**翻页=换 tag 隐式重置**（SessionQuotaHook
同款技巧）；afterModel 计量入账（tokens + cost-micro 双轨，`<tag>:<cumulative>`
CAS 单键——usage 提取与 TokenBudgetHook 同口径）；beforeModel 闸：本周期
累计达 tokens-limit 或 cost-limit 即 block（下一调用拦截——不可逆预算纪律）；
warning-percent 软预警一次一发（338 同语义按周期域）。状态挂合成会话
`__buzhou.period-budget__`（archive/eval 合成会话先例）。yml
`buzhou.budget.period.{enabled,unit,tokens-limit,cost-micro-usd-limit,
warning-percent}`。

## Notes

- 号段：spec 408 / T707–T708 / impl-381。
- 借鉴源：AWS Budgets（period-based budget + auto reset）；翻页机制借
  SessionQuotaHook epochDay 模式推广到月/周。
- 纪律：全局预算是单进程语义（多实例=N 份——warnIfMultiInstance 同口径
  告警归宿主 runbook；共享后端扩散候选）；成本轨无价目配置时记 0（诚实：
  无价目不伪造成本）。

## Out of scope

- 跨实例共享周期预算（Redis 后端扩散候选）；per-key 周期（VirtualKeys
  扩散）；周期翻页事件（翻页是隐式的——真需求再显式化）；预算回落解封
  （预算不可逆——同会话硬顶纪律）。

## Tickets

- [x] [T707 PeriodBudgetHook 计量+闸](../tickets/T707-period-budget-hook.md)
- [x] [T708 yml 装配 + 软预警](../tickets/T708-period-budget-assembly.md)
