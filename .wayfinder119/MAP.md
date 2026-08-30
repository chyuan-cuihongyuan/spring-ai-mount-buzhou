# Wayfinder Map — Buzhou key 级预算闸（effort #119，A 会话第 14 轮）

> **A 侧票号自 T501 起（B 侧预留 T471-T500——本轮起避让）**；spec 偶数段
> 沿用。借鉴 LiteLLM virtual-key 超限闸位语义。

## Destination

spec 124 VirtualKeys 接进 TokenBudgetHook：key 预算跨会话共享、扣减随 usage
入账、越限拦截下一次调用（观测事件即刻发）、窗口 reset 恢复。

## Notes

- 耗尽态语义（isExhausted）：已用 ≥ 限额，或上次扣减越限被拒——「部分消耗
  永远凑不满限额」的诚实表达（150/200 类不死循环）；reset 同清。

## Decisions so far

- [key 级预算闸](tickets/T501-key-budget-gate.md) — 5 参构造兼容 + 双面接线
  （afterModel 扣减+事件 / beforeModel 拦截）。

## Not yet specified

- autoconfig yml 键（buzhou.token-budget.virtual-key）；多 key 按会话路由。

## Out of scope

- 成本面（micro-USD）key 配额；key 解析动态面（租户→key 映射）。

## Tickets

- [x] [T501 key 级预算闸](tickets/T501-key-budget-gate.md)（impl-286）
- [x] [T502 收口提交](tickets/T502-key-budget-gate-close.md)（impl-286）
