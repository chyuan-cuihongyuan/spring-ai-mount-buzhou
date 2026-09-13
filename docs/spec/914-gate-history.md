# 914 — gate 判定环形历史读面

> 来源：I 会话第 15 轮 = effort #914（[T1279](../../.wayfinder/tickets/T1279-gate-history-shape.md) / [T1280](../../.wayfinder/tickets/T1280-gate-history-verify.md) / impl 667）。借鉴：K8s Events 事件史——判定留痕、新→旧可读、有界保留。

## Problem Statement

`EvalGate.enforce` 每次判定即返回 `GateResult`——判定轨迹不留痕。「最近拒了几次 / 通过率趋势 / 阈值是否定严了」无现成读面；run 结果虽落盘（spec 52），但「门判定」这个动作本身（阈值×通过率→判定）是独立语义，散落在调用方日志里。

## 目标

- `EvalGate` 实例环形历史：
  - `record GateDecision(Instant at, String datasetName, String runId, double threshold, double passRate, boolean passed)`；
  - `HISTORY_CAPACITY = 16`（static final；超限丢最旧——有界纪律）；
  - `enforce` 尾部记录判定（synchronized 单点）；`history()` 返回新→旧不可变快照；
  - 内存有界、无持久化（run 记录持久化归 spec 52 口径不变）；
- 既有 `enforce` 返回语义零变化（多了历史副作用——只读消费，不改判定）。

## 兼容性

纯增量：公共类新增方法 + 公共嵌套 record；既有判定行为零变化。
