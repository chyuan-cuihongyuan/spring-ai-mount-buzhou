# 1033 — token 估算调用量与总量读面

> 来源：J 会话第 34 轮 = effort #1033（[T1519](../../.wayfinder/tickets/T1519-token-estimate-stats-shape.md) / [T1520](../../.wayfinder/tickets/T1520-token-estimate-stats-verify.md) / impl 786）。与 R16/R30 同族：纯函数判定路径的进程级调用量显形（预算面可观测性）。

## Problem Statement

CharHeuristicTokenEstimator（4 字符/token 启发式）是预算计算（spec 147/20/60 族的估算基准）的事实口径，但 estimate/estimateMessages 调用零计数：**预算面吃的估算总量与调用量**不可见——估算调用量突增即预算路径被高频触发的信号（压缩饥饿定位线索）。

## 目标

- `CharHeuristicTokenEstimator` 增量（core/token，静态进程级——调用点内联构造故实例计数无意义，先例 R1 ToolPolicyMatcher/R15 Spotlighting）：`estimateCalls` / `batchCalls` / `totalEstimatedTokens` 三 AtomicLong。
- 嵌套 record `TokenEstimateStats(long estimateCalls, long batchCalls, long totalEstimatedTokens)` + `stats()` + `resetForTest()`。
- estimate/estimateMessages 返回值逐位不变（仅加计数）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 按调用方分桶（调用点分散，无稳定标识）。
- 估算精度校准面（语义另立项）。
