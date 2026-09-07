# Wayfinder Map — Buzhou 预算软预警线（effort #338，C 会话第 39 轮）

> C 会话第 39 轮。预算闸（spec 16/148）只有硬顶：撞墙才知道——墙上
> 之前零信号。AWS Budgets / 云账单预警的常识：消耗达限额百分比（如
> 80%）先预警一次，人到墙前还有时间反应（加额度/收束/换 key）。

## Destination

TokenBudgetHook 软预警：`buzhou.token-budget.warning-percent`（默认 80、
-1 关闭）——会话总量/会话成本/虚拟 key 三维消耗达硬顶该百分比即发
`budget.warning` 事件（一次一发——消耗单调 warned 即终局；仅事件不拦
截，硬顶闸照旧）+ `buzhou.budget.warnings` 计数。

## Notes

- 号段：spec 338 / T667–T668 / impl-361。
- 借鉴源：AWS Budgets（percent-of-budget alert——到墙之前先叫人）。
- 纪律：默认 80 仅加事件零拦截（控制流零变化）；已耗尽 key 走
  hard-stop 语义不再预警。

## Decisions so far

- 预警标记 per session×dimension / per key×dimension（会话总量与成本
  各一发）；内存标记 1024 上限诚实降级。
- 判定整数化：value×100 ≥ limit×percent（无浮点）。

## Out of scope

- 预警回调/通知路由（事件面已够——312/330 告警族可消费）；
- 多级预警线（50/80/95 阶梯——需求出现再加）。

## Tickets

- [x] [T667 软预警逻辑（三维判定+一次一发）](tickets/T667-budget-warning.md)
- [x] [T668 属性面 + 收口](tickets/T668-warning-props.md)
