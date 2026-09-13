# 827 — 定价表覆盖审计

> 来源：H 会话第 28 轮 = effort #827 / [T1155](../../.wayfinder/tickets/T1155-pricing-coverage-audit.md) / [T1156](../../.wayfinder/tickets/T1156-pricing-coverage-audit-verify.md) / impl 580。
> 借鉴：LiteLLM model_prices 覆盖思想（≈28K star）。

## Problem

新模型上线价目表没配：调用照常、成本归因静默计 0——预算失真无人察觉。「哪些被调用的模型没有价」缺对账面。

## Solution

`PricingCoverageAudit`（core.budget，纯函数）：

- **三层匹配**：精确 → 忽略大小写 → 剥 `provider/` 前缀再比（LiteLLM 形态）。
- **报告**：calledModels/coveredModels/coverageRatio + unknown 典序封顶 32+溢出汇总行。
- **空真**：无调用 → 覆盖率 1.0；价表空 → 全 unknown 比率 0。

## 兼容性

纯新增（键集由调用方自 PricingTable 采集——零侵入）。

## 诚实边界

时点对账（热载后需重审）；不做通配展开；前缀剥离单层。
