# Spec 408 — 预算日历周期（effort #408）

> wayfinder map：`.wayfinder/maps/effort-408.md`（T707–T708）。D 会话第 9 轮。

## Problem Statement

预算面三态各有缺口：会话级硬顶（无跨会话概念）、虚拟 key 预算（总量无
翻页）、日配额（仅日粒度）。「这个月全进程最多花 X」的**账期预算**没有
承载——月度账单对不上预算闸。

## Solution

`core.budget.PeriodBudgetHook`（AWS Budgets calendar period 借鉴）：

- **周期**：unit = MONTHLY（`YearMonth` tag）/ WEEKLY（epochWeek）/ DAILY
  （epochDay）；periodTag 入状态键——**翻页 = 换 tag 隐式重置**。
- **计量**（afterModel）：usage 提取与 TokenBudgetHook 同口径（替身模型/
  无计量不误记）；tokens 与 cost-micro 双轨累计——`<periodTag>:<cumulative>`
  单键 CAS（SessionQuotaHook 同款带进度检测重试）；无价目配置时成本轨
  记 0（诚实：无价目不伪造成本）。
- **闸**（beforeModel）：本周期累计达 tokens-limit 或 cost-micro-usd-limit
  → `block`（下一调用拦截，不可逆预算纪律）；事件
  `budget.period.exceeded`。
- **软预警**：warning-percent（默认 80）一次一发（warned 集 1024 上限
  诚实降级——338 同语义按周期域）；事件 `budget.period.warning`。
- 状态挂合成会话 `__buzhou.period-budget__`（archive/eval 先例）；
  yml `buzhou.budget.period.{enabled=false, unit=monthly, tokens-limit,
  cost-micro-usd-limit, warning-percent}`——两 limit 均未配 fail-fast
  （开预算闸却没限额是配置错误）。

## User Stories

1. 作为平台运维，我想声明月度全进程 token/成本预算，所以 月度账单
   与预算闸同一口径。
2. 作为运维，我想周期翻页自动重置，所以 每月零操作回归满额。
3. 作为 SRE，我想软预警先于硬顶一次一发，so 烧穿前有行动窗口。

## Implementation Decisions

- 全局预算单进程语义（多实例 = N 份——runbook 口径；共享后端扩散候选）。
- 计量不 block（响应已生成）；闸在下一次 beforeModel。

## Testing Decisions

- 计量累计（双轨）+ tag 翻页重置（Clock 注入换周期）；
- tokens 闸与 cost 闸各自治闸 + block；
- 软预警一次一发 + 未配 limit fail-fast + yml 装配默认关。

## Out of Scope

- 跨实例共享；per-key 周期；翻页事件；预算回落解封。

## Further Notes

- 新公共类型 `PeriodBudgetHook` / `BuzhouPeriodBudgetProperties` 随轮
  regenerate 快照。
