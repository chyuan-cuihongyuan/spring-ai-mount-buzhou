# Spec 521 — 事故复盘一键包（effort #521）

> wayfinder map：`.wayfinder/maps/effort-521.md`（T793–T794）。E 会话第 22 轮。

## Problem Statement

事故复盘要手工凑三面数据（405 时间线/83 错误签名/334 成本 rollup）——
无标准打包。317 ExportBundle 提供 ZIP+manifest 对账底座，缺事故域预设
组合。

## Solution

`core.export.PostmortemBundle`（builder 流式装配三可选源）：

- compose(zip) → 标准命名源：timeline.jsonl（405 状态变迁）/
  error-signatures.jsonl（83 top 族）/cost-{model,virtual-key}.jsonl
  （334 双维 rollup）/summary.json（计数与成本总览）。
- 源缺席跳过不中断（317 单源故障隔离同语义）；manifest 对账免费获得。

## User Stories

1. 作为值班，我想一个调用导出复盘 ZIP， so 复盘材料在事故中后期即可
   归档（时间线/签名/成本一站齐）。

## Implementation Decisions

- 只聚合既有观测面快照（不采集新数据）；行内容为既有有界面
  （timeline 256/签名 256/rollup 64 值封顶）。

## Testing Decisions

- 三源齐打包（manifest 条目+ZIP 内容+summary）；源缺席跳过仅 summary。

## Out of Scope

- 自动触发；采集新数据。

## Further Notes

- 新公共类型 `PostmortemBundle` 随轮 regenerate 快照 + api-surface.md 加行。
