# Spec 188 — 成本账单导出（effort #138）

> wayfinder map：`.wayfinder138/MAP.md`（T548–T549）。导出族第八员
> （spec 174 fog「JSONL 账单导出」收口）。

## Problem Statement

成本台账（spec 174/176 已自动入账）只有进程内 topByCost——账单复盘的窗口
时序分析需要导出面与导出族汇流。

## Solution

`budget/ModelCostLedgerJsonl.export(ledger, writer)`：全部在册成本平铺一行一
JSON，与 topByCost 同序；**双口径列**——`microUsd` 整数（精确口径，OLAP 聚合
用它）+ `usd` 6 位小数字符串（人读口径）。export → reset 循环 = 每窗口一份
账单；空表零行诚实。

## User Stories

1. 作为财务，账单进 DuckDB 按窗口环比，所以「哪个模型烧钱涨了」一查即出。

## Testing Decisions

- 红队：同序 + 双口径列值；空表零行 + 窗口 reset 循环。

## Out of Scope

- gzip；合流打包；per-tenant。

## Further Notes

- 双口径列先例：一份导出同时喂机器（精确）与人（可读）。
