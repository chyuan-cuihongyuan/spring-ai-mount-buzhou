# Wayfinder Map — Buzhou 模型成本台账（effort #131，A 会话第 26 轮）

> A 侧票号 T501+ / spec 偶数段沿用。借鉴 WandB/Langfuse cost tracking。

## Destination

per-model micro-USD 全局账本：累计 + 排行 + 总数 + 窗口 reset——「哪个模型
在烧钱」的账单事实源（与 per-session 成本闸正交）。

## Notes

- 整数 micro-USD（spec 16 同口径）；64 封顶折 __overflow__；零成本也记。

## Decisions so far

- [ModelCostLedger](../tickets/T529-cost-ledger.md) — record/topByCost/
  totalMicroUsd/reset + 全局旋钮。

## Not yet specified

- TokenBudgetHook 打点接线（afterModel 价目换算处一行）；JSONL 账单导出。

## Out of scope

- per-tenant 拆列；价目表管理。

## Tickets

- [x] [T529 模型成本台账](../tickets/T529-cost-ledger.md)（impl-298）
- [x] [T530 收口提交](../tickets/T530-cost-ledger-close.md)（impl-298）
