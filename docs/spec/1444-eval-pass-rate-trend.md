# 1444 — 评估通过率趋势审计

> 来源：L 会话第 44 轮 = effort #1444（票 T2189 / T2190 / impl 1096）。借鉴：Theil–Sen 稳健回归（成对斜率中位数——单点噪声不扭曲趋势方向，scikit-learn 同源实现）。

## Problem Statement

单次 run 的 pass@1 有噪声，「这版提示词/这版模型在变好还是变坏」需要**跨 run 趋势**读面：门判定（GateDecision）给瞬时过/不过，方向（改进/退化/稳定）无审计——退化趋势连续三轮才被人眼发现。

## 目标

- `EvalPassRateTrend`（core/eval，纯函数静态面，private 构造）：
  - `analyze(List<Double> passRates)`（时间序 0..1）→ `record TrendReport(runs, passRates, slopeMedian, direction)`；
  - **Theil–Sen 稳健斜率**：全部成对斜率的中位数（对离群 run 抗噪——均值斜率会被单点拉偏）；斜率单位 = 每 run 一个百分点；
  - 方向闭集 `enum Direction { INSUFFICIENT, DEGRADING, STABLE, IMPROVING }`：死区 `STABLE_EPSILON=0.005/run`；run <2 INSUFFICIENT；
  - 纯函数零状态：不触 EvalGate/registry。

## 兼容性

纯函数零 IO；趋势只读不裁决（门判定归 EvalGate）。

## Out of Scope

- 显著性检验（Mann-Kendall p 值——工程死区判据先满足）。
- 多数据集/多评估器维度拆分（单序列口径显式）。
- 自动回滚联动（读面不裁决）。
