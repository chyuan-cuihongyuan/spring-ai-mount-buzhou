# Spec 314 — 价目快照随单（effort #314）

> wayfinder map：`.wayfinder/maps/effort-314.md`（T619–T620）。借鉴：复式记账
> （账单自含计价事实——fog 152「价目快照随单」项）。

## Problem Statement

成本台账只记金额：价目配置变更后，历史账单无法复算（缺「当时单价」）；
合规对账与成本回归分析没有计价事实。

## Solution

`ModelCostLedger` 行级价目快照：

- `record(model, costMicroUsd, pricingSnapshot)` 三参重载（旧两参 = 无快照
  兼容）；`PricingSnapshot(inputPerMillion, outputPerMillion)` BigDecimal
  原口径，null 表示无价目（零成本行）。
- 快照语义 = 该模型<b>最近一次记账时</b>的单价（聚合面诚实边界：行级逐笔
  计价归事件流）。
- JSONL 账单行补 `inputPerMillion` / `outputPerMillion` 两列（有快照时）。
- `TokenBudgetHook` 记账时传当前价目（接线点）。

## User Stories

1. 作为财务，价目调价后旧账单仍可离线复算——行内单价自含。
2. 作为运维，无价目模型成本行不带单价列（零成本事实不被伪价污染）。

## Implementation Decisions

- per-model 最后价快照（不做版本链——版本表归 reloadable 族）。

## Testing Decisions

- `PricingSnapshotLedgerTest`：快照随单存取 / 调价后行价更新 / JSONL 单价
  列（有/无快照两态）/ 旧两参兼容 / hook 接线后行价在册。

## Out of Scope

- 逐笔记价事件；价目版本链订阅。

## Further Notes

- 成本族：台账（16/131-132）/ 健康面（192/139）/ 弹性池（157）/ **价目随单
  （本轮）**。
