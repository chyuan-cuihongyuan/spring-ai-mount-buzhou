# Wayfinder Map — Buzhou 成本台账接线（effort #132，A 会话第 27 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 174 fog「Hook 打点接线」收口。

## Destination

TokenBudgetHook afterModel 价目换算处一行入账 ModelCostLedger——零配置
（有 token 预算钩子即有全局成本账）。

## Notes

- 零成本也记（无价目模型在册零值行——「跑过零成本」是账单事实）；
  只记账不拦截。

## Decisions so far

- [台账接线](../tickets/T532-ledger-wiring.md) — afterModel 单点 + e2e 两面
  （价目累计跨会话 / 无价目零值在册）。

## Not yet specified

- JSONL 账单导出（导出族第八员）。

## Out of scope

- per-tenant 拆列；预算钩子关闭时的旁路记账（enabled=false 全旁路一致）。

## Tickets

- [x] [T532 台账接线](../tickets/T532-ledger-wiring.md)（impl-299）
- [x] [T533 收口提交](../tickets/T533-ledger-wiring-close.md)（impl-299）
