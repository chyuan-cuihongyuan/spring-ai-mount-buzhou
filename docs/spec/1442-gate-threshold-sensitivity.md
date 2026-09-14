# 1442 — 评估门阈值敏感性扫描

> 来源：L 会话第 42 轮 = effort #1442（票 T2185 / T2186 / impl 1094）。借鉴：scikit-learn validation_curve（超参敏感性曲线——参数微小移动导致的大幅波动即脆弱）。

## Problem Statement

评估门 PASS 线（threshold，spec 513 族）定得准不准无读面：分数密集带贴近阈值时，分数噪声直接决定过/不过——「门立在分数稀疏带还是密集带」是门的稳健性指标，无扫描工具。

## 目标

- `GateThresholdSensitivity`（core/eval，纯函数静态面，private 构造）：
  - `analyze(List<Double> scores, double threshold, double delta)` → `record SensitivityReport(totalScores, bandCount, tightenFlips, loosenFlips)`；
  - δ 带 = [threshold−δ, threshold+δ)（左闭右开）；PASS 语义 = score ≥ threshold（与 EvalGate 同向）；
  - `tightenFlips`（上调 δ 将翻 FAIL 的当前 PASS）/ `loosenFlips`（下调 δ 将翻 PASS 的当前 FAIL）——方向分向；
  - `sensitivityRatio` 派生（带内占比，越低越稳健；0 分数 -1 哨兵）；δ 负值 fail-fast。
- 纯函数零状态：不触 EvalGate 判定（离线扫描面）。

## 兼容性

纯函数零 IO；PASS 语义与既有门同向。

## Out of Scope

- 自动阈值寻优（离线扫描面，寻优归调参域）。
- 多阈值扫描曲线（单 δ 带口径；曲线化另轮）。
- EvalGate 历史联动（环形史 I T1279 已有）。
