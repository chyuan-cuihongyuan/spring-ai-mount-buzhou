# 1411 — 取消延迟追踪读面

> 来源：L 会话第 12 轮 = effort #1411（票 T2123 / T2124 / impl 1064）。借鉴：Temporal cancellation latency（取消语义的正确性看「发出信号→实际停止」的时延——即时取消是承诺，时延分布是兑现证据）。与 H 824 取消原因分布辨义：那轴是「为何取消」，本轴是「取消后多久真停」。

## Problem Statement

`cancel(mode, cause)` 发出即返回（requestCancel + 事件 + 指标），但**在途轮次的实际终结是异步的**（流式经 doFinally→failTurnOnce 链路）。「取消后多久真停」无读面：挂死的模型调用让取消延迟从毫秒劣化到超时级，宿主无从感知承诺失守。

## 目标

- `CancelLatencyTracker implements SessionObserver`（core/session，opt-in 实例面，同实例注册全会话）：
  - `onTurnStart`：标记该会话「轮在途」；`onCancel`：仅在途时记录取消请求时刻（无轮取消——轮间操作——不入账）；
  - `onTurnEnd/onTurnError`：存在未决取消 → 延迟 = now − 请求时刻 入环并清键（会话内单飞保证无并发轮竞态）；
  - 有界环 64 样本（StoreLatencyRing 口径先例）+ `stats()`：`record CancelLatencyStats(long tracked, long p50Millis, long p95Millis, int pendingCancels)`（recent-rank 分位）+ `resetForTest()`。

## 兼容性

纯 opt-in 读面：不注册零开销；observer 全 default 无既有实现感知；不触 cancel/终结语义。

## Out of Scope

- 非流式轮的取消延迟（同步 `.call()` 无外部取消缝——诚实入档）。
- 取消后轮未终结的超时判定（TurnStallWatchdog 域）。
- 按 cause 分桶延迟（样本量小，先看总量）。
