# Wayfinder Map — Buzhou 成本预测外推（effort #403，D 会话第 4 轮）

> D 会话第 4 轮。勘察（2026-09-08）：成本族有台账（ModelCostLedger 累计
> 值）、价目快照随单（314）、软预警线（338——现值对阈值）、归因台账
> （334）——但**全部是「已花多少」**；无时间序列、无速率、无外推。
> 「照这个烧法月底超预算吗」这个运维最常问的问题没有答案。

## Destination

`core.budget` 时间序列速率 + 外推（AWS Budgets forecast 借鉴——
actualSpend/elapsed × total 的诚实线性外推）：
`SpendRateRing`（分钟桶环形窗，Clock 可注入）+ `CostForecast`（窗口
花费/小时速率/水平线外推/预算对照 projectedOver）+ `ModelCostLedger`
**监听缝**（addListener——记账即喂数，单点不重复算成本）+
`CostForecastHealth`（机制名 cost-forecast，恒 UP 预测面——超预算是
预测不是事故；无预算 UNKNOWN 与 348 同口径）。yml
`buzhou.budget.forecast.{enabled,window,horizon,budget-micro-usd}`
声明即装配。

## Notes

- 号段：spec 403 / T697–T698 / impl-376。
- 借鉴源：AWS Budgets forecast（实际/已过 × 全期）——线性外推的诚实
  边界文档化（不建模趋势拐点；窗口内速率代表未来是假设）。
- 纪律：监听缝挂 ModelCostLedger 单点（TokenBudgetHook 已在此打点——
  不在 hook 里二次算成本）；桶环有界（窗长即内存上界）。

## Out of scope

- 趋势回归/拐点检测（线性外推够用论——真需求再议）；超预算告警事件
  （338 阈值域管现值；预测阈值告警待 312 族消费 details 的真需求）；
- per-key/per-model 分域预测；持久化历史（重启清零为诚实边界）。

## Tickets

- [x] [T697 SpendRateRing + CostForecast](../tickets/T697-spend-rate-forecast.md)
- [x] [T698 监听缝 + 健康面 + yml 装配](../tickets/T698-cost-forecast-health.md)
