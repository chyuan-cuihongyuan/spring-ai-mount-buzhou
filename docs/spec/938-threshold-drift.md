# 938 — gate 阈值漂移读面

> 来源：I 会话第 38 轮 = effort #938（[T1319](../../.wayfinder/tickets/T1319-threshold-drift-shape.md) / [T1320](../../.wayfinder/tickets/T1320-threshold-drift-verify.md) / impl 690）。spec 914 历史面的聚合视图——「CI 红了就调阈值」的流程不健康信号显形。

## 背景

`EvalGate.history()`（spec 914）留痕判定序列，阈值调整（同一数据集多次判定的 threshold 不同）是流程不健康信号——被显形后可触达「阈值治理」话题。

## 目标

- `EvalGate.thresholdDrift(List<GateDecision> history)` 静态纯函数：
  - 相邻判定 threshold 不同 = 1 次调整；返回 `record ThresholdDrift(int transitions, int sampled)`（sampled = history.size() − 1）；
  - 空/单条返回 0 次调整（sampled = 0）；
  - 纯函数零副作用（history 入参只读）；
- 既有 enforce/history 零变化。

## 兼容性

纯增量：公共类新增静态方法 + 公共嵌套 record，零既有行为变化。
