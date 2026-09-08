# Spec 403 — 成本预测外推（effort #403）

> wayfinder map：`.wayfinder/maps/effort-403.md`（T697–T698）。D 会话第 4 轮。

## Problem Statement

成本族全部回答「已花多少」（台账/价目随单/软预警/归因），无时间序列、
无速率、无外推——「照这个烧法到水平线终点会不会超预算」没有答案，
超支发现只能等 338 阈值真被撞上（为时已晚）。

## Solution

`core.budget` 速率时间序列 + 线性外推（AWS Budgets forecast 借鉴——
actualSpend / elapsed × total）：

- **`SpendRateRing`**：分钟桶环形窗（默认 1h 窗 = 60 桶；Clock 可注入
  ——测试确定性）。`record(microUsd)` 记入当前分钟桶（跨桶滚动清零
  旧桶）；`windowTotal(lookback)` 窗内合计；`ratePerHour(lookback)`
  外推为小时速率。内存上界 = 桶数（有界纪律）。
- **`CostForecast`**（record）：windowMicroUsd（窗内花费）、
  ratePerHourMicroUsd（小时速率）、horizonMicroUsd（速率 × 水平线）、
  budgetMicroUsd、projectedOver（水平线外推 ≥ 预算）。**诚实边界**：
  线性外推不建模趋势拐点——「窗口内速率代表未来」是假设，文档化。
- **`ModelCostLedger` 监听缝**：`addListener(Consumer<ModelCost>)`——
  记账即喂数（TokenBudgetHook 已在 ledger 打点，预测器不二次算成本；
  OVERFLOW 折叠行同样喂）。
- **`CostForecastHealth`**（机制名 `cost-forecast`）：恒 UP——超预算
  是预测不是事故（348 同口径：DOWN 会误导）；details = 窗内花费/
  小时速率/水平线外推/预算/projectedOver；未配预算（≤0）UNKNOWN
  （机制半配置）。
- yml：`buzhou.budget.forecast.{enabled, window, horizon,
  budget-micro-usd}` 声明即装配（监听全局 ledger；重启历史清零为
  诚实边界）。

## User Stories

1. 作为运维，我想看到当前烧钱速率与水平线外推值，所以 「会不会超
   预算」在期中就有答案而不是撞线才知道。
2. 作为 SRE，我想预测面恒 UP,所以 预测超支不会被误判为事故触发
   重启/摘流量。
3. 作为宿主，我想未配预算时面 UNKNOWN,所以 半配置不误报。

## Implementation Decisions

- 监听缝挂 ModelCostLedger 单点（不在 hook 二次算成本）。
- 分钟桶环形：无持久化（重启清零——进程内观察面口径）。

## Testing Decisions

- 桶环：跨分钟分桶、旧桶滚动清零、窗内合计/速率数值驱动；
- forecast：速率 × 水平线、projectedOver 边界（≥）；
- 健康面：details 数值、无预算 UNKNOWN；
- 监听缝：record 触发 listener（含 OVERFLOW 路径）+ yml 装配。

## Out of Scope

- 趋势回归/拐点；预测阈值告警事件；per-key 分域；持久化历史。

## Further Notes

- 新公共类型 `SpendRateRing` / `CostForecast` / `CostForecastHealth` /
  `BuzhouCostForecastProperties` 随轮 regenerate 快照。
