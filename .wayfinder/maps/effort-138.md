# Wayfinder Map — Buzhou 成本账单导出（effort #138，A 会话第 33 轮）

> A 侧票号 T501+ / spec 偶数段沿用。导出族第八员。

## Destination

ModelCostLedgerJsonl：双口径列（microUsd 精确 / usd 6 位小数人读）——
export → reset 每窗口一份账单。

## Decisions so far

- [账单导出](../tickets/T548-cost-export.md) — 静态面同族模式。

## Not yet specified

- gzip 面；与观测导出合流（同窗口多文件打包）。

## Out of scope

- per-tenant 列；价目快照随单。

## Tickets

- [x] [T548 账单导出](../tickets/T548-cost-export.md)（impl-305）
- [x] [T549 收口提交](../tickets/T549-cost-export-close.md)（impl-305）
