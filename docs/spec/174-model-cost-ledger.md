# Spec 174 — 模型成本台账（effort #131）

> wayfinder map：`.wayfinder131/MAP.md`（T529–T530）。借鉴：WandB/Langfuse
> cost tracking（账单按模型分组、窗口对比）。

## Problem Statement

成本可观测只有 per-session 硬顶与事件——「全局哪个模型在烧钱、本窗口 vs
上窗口涨了多少」没有事实表，账单复盘要全量 join 事件。

## Solution

`budget/ModelCostLedger`：per-model micro-USD 累计的进程内有界表（64 封顶，
新名折 `__overflow__`）。`record`（整数 micro-USD——spec 16 同口径，零成本
也记）；`topByCost(n)` 排行（microUsd 降序 + 名字典序稳定）；
`totalMicroUsd()` 总数（含 overflow）；`reset()` 窗口清零（export → reset
循环）。全局旋钮模式（TokenBudgetHook 打点接线为后续 fog）。

## User Stories

1. 作为财务，我按窗口导出台账，所以「哪个模型烧钱、环比涨多少」一张表回答。
2. 作为架构师，排行前几名即替换/议价候选，所以降本动作有准星。

## Testing Decisions

- 红队：累计与稳定排行（零成本在册）；封顶折入 + 既有继续累计 + total 含
  overflow；reset 清零 + 参数 fail-fast。

## Out of Scope

- Hook 打点接线；JSONL 导出；per-tenant 拆列；价目管理。

## Further Notes

- 预算面第四块：session 闸（spec16）/ key 配额（spec148）/ 全局账本
  （本表——只记账不拦截）。
