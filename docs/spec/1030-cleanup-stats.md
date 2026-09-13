# 1030 — 会话级联清理聚合计数读面

> 来源：J 会话第 30 轮 = effort #1030（[T1511](../../.wayfinder/tickets/T1511-cleanup-stats-shape.md) / [T1512](../../.wayfinder/tickets/T1512-cleanup-stats-verify.md) / impl 783）。与 R28 同族：逐次结果 → 跨调用聚合水位（PostgreSQL autovacuum stats 思想）。

## Problem Statement

SessionCleaner（impl-35，五槽 + 恢复设施 + 贡献者级联清理）逐次返回 SessionCleanupResult（cleaned/failures 明细），但无跨调用聚合：删了多少会话、哪个清理目标反复失败无水位——单 store 实现持续故障（如 JDBC 目标连续失败）被逐次 ERROR 日志稀释，无量化告警依据。

## 目标

- `SessionCleaner` 增量（core/cleanup，实例级）：`deleteCalls` / `cleanedTargets` / `failedTargets` 三 AtomicLong + `failuresByTarget` 分桶（ConcurrentHashMap——目标名固定集合，有界）。
- 嵌套 record `CleanupStats(long deleteCalls, long cleanedTargets, long failedTargets, Map<String, Long> failuresByTarget)` + `stats()` 快照（守恒：cleanedTargets + failedTargets == 实际执行的目标删除次数）。
- 清理行为逐位不变（逐目标隔离/失败聚合/日志零变化）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 失败升级上抛策略变化（既有「调用方决定上抛」不变）。
- 跨实例聚合（单实例协调器口径）。
