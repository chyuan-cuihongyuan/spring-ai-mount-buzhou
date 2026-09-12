# Spec 508 — 成本异常尖峰检测（effort #508）

> wayfinder map：`.wayfinder/maps/effort-508.md`（T767–T768）。E 会话第 9 轮。

## Problem Statement

成本面有台账/预算硬顶/线性预测（403）——速率在预算内但相对自身基线
**突刺**（正常速率 10 元/h 突然 5000 元/min）不可见。403 只回答趋势
（会不会超预算），不回答「现在烧得正不正常」。Prometheus/Istio 滚动
基线异常检测空白。

## Solution

`budget.CostSpikeDetector` + `BuzhouCostSpikeProperties`：

- **检测**：当前分钟桶 vs 基线（前 `baseline-buckets` 个已完成桶，仅计
  本进程写入过的桶）均值/标准差 → z = (current−mean)/std；z ≥ 阈值
  （默认 3.0）且 current ≥ 绝对地板（默认 10_000 microUsd——零附近
  噪声兜底）且有效基线桶 ≥ minSamples（默认 10）→ 尖峰。
- **产出**：`SpikeEvent`（current/mean/std/z）listener 回调 +
  `buzhou.budget.cost-spike` 计数 + cooldown 防抖（默认 5min 一发）。
- **喂缝**：与 403 同——`ModelCostLedger.global().addListener` 单点喂数；
  评估仅在 record 时触发（零花费零成本）。
- **yml**：`buzhou.budget.spike.{enabled, baseline-buckets, min-samples,
  z-threshold, floor-micro-usd, cooldown}`——enabled 默认关（opt-in，
  403 同族）。

## User Stories

1. 作为成本运维，我想在费率相对自身基线突刺时收到事件， so 死循环
   agent/重试风暴在预算烧穿前就被发现。
2. 作为宿主，我想检测有噪声地板与防抖， so 正常波动不制造告警疲劳。

## Implementation Decisions

- 零方差基线（全 0 或全等值）：current>mean 视为 z=∞（地板与
  minSamples 兜噪）。
- 重启历史清零（进程内观察面——403 同注记）。
- 只检测不拦截：335 预算冻结可作下游联动，职责分离。

## Testing Decisions

- Clock 注入：喂满基线（minSamples 个小额分钟桶）→ 大额分钟桶触发
  listener/计数；样本不足不触发；地板下不触发；冷却窗口内只发一次。
- yml：enabled=true 装配+listener 收账；缺席无 bean。

## Out of Scope

- 多维尖峰（per-model/tenant）；季节性基线；自动降级联动。

## Further Notes

- 新公共类型 `CostSpikeDetector`、`BuzhouCostSpikeProperties` 随轮
  regenerate 快照 + api-surface.md 加行。
