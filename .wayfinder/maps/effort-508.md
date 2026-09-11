# Wayfinder Map — Buzhou 成本异常尖峰检测（effort #508，E 会话第 9 轮）

> E 会话第 9 轮。勘察：成本面有台账（174/334）/预算硬顶（16）/预测
> 外推（403 线性）/告警规则（312）——**统计异常检测**空白：速率在预算
> 内但相对自身基线突刺（z-score）不可见（403 只回答「会不会超预算」，
> 不回答「现在烧得正不正常」）。Prometheus/Istio 异常检测思想。

## Destination

`budget.CostSpikeDetector`（复用 SpendRateRing 分钟桶环——record 喂缝与
403 同：ModelCostLedger.global().addListener 单点喂数）：当前分钟桶 vs
基线（前 N 个已完成桶均/标差，仅计本进程存活写入过的桶）→ z ≥ 阈值
（默认 3.0）且当前桶 ≥ 绝对地板（默认 10_000 microUsd 噪声地板）且
minSamples 满足（默认 10）→ SpikeEvent（current/mean/std/z）listener
回调 + `buzhou.budget.cost-spike` 计数 + cooldown 防抖（默认 5min 一发）。
`BuzhouCostSpikeProperties`（buzhou.budget.spike.* enabled 默认关）。
评估仅在 record 时触发（零花费零成本）。

## Notes

- 号段：spec 508 / T767–T768 / impl-411。
- 借鉴源：Prometheus/Istio 异常检测（滚动基线 z-score）、AWS Budgets
  anomaly（与 403 forecast 互补：forecast 回答趋势、spike 回答突刺）。
- 诚实边界：零方差基线（全 0 或全等值）下 current>mean 即视为 z=∞
  （地板与 minSamples 兜噪）；重启历史清零（进程内观察面——403 同注记）；
  只检测不拦截（335 冻结可作下游联动）。

## Out of scope

- 多维（per-model/tenant）尖峰；季节性基线；自动降级联动。

## Tickets

- [x] [T767 基线 z-score 检测](../tickets/T767-cost-spike-detector.md)
- [x] [T768 防抖与装配](../tickets/T768-cost-spike-assembly.md)
