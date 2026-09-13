# 1038 — 摘要桥操作与代数回退读面

> 来源：J 会话第 38 轮 = effort #1038（[T1529](../../.wayfinder/tickets/T1529-summary-bridge-stats-shape.md) / [T1530](../../.wayfinder/tickets/T1530-summary-bridge-stats-verify.md) / impl 790）。与 R28 同谱系：摘要管线的跨调用聚合水位（单调代数回退探测——K8s 事件聚合 + 单调水位思想）。

## Problem Statement

SummaryStoreBridge（spec 20/95，摘要版本化追加存取桥）save/loadLatest 零计数：generation 本应单调不减（版本化追加语义），**回退即摘要被旧快照覆盖的异常**（R28 手动压缩 / R21 自动压缩双写方竞争的唯一可视化点）——现无任何读数。

## 目标

- `SummaryStoreBridge` 增量（buzhou-memory summary 包，实例级）：`saves` / `loads` / `generationRegressions` 三 AtomicLong + per-session lastGeneration 有界 LRU（1024，TurnTimingHook 同先例）。
- 嵌套 record `SummaryStoreStats(long saves, long loads, long generationRegressions)` + `stats()` 快照。
- save/loadLatest 返回值与存储语义逐位不变（仅加计数与比较）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 回退升级阻断/恢复旧摘要（语义变化另议——本轮只显形）。
- 按段（section）细分统计。
