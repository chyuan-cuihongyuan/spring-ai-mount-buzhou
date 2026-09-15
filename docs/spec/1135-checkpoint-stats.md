# 1135 — 压缩检查点操作读面

> 来源：J 会话第 115 轮 = effort #1135（[T1689](../../.wayfinder/tickets/T1689-checkpoint-shape.md) / [T1690](../../.wayfinder/tickets/T1690-checkpoint-verify.md) / impl 873）。借鉴：数据库 savepoint 使用率（安全网机制的触发分布是风险对账基座）。memory/compact 域第三轴。

## Problem Statement

`CompactionCheckpoints`（压缩前检查点 + 三档回滚）操作零计数——**检查点保存量与回滚量不可见**：安全网机制被触发的分布（保存频繁=压缩频繁；回滚高发=压缩事故）无对账。

## 目标

- `CompactionCheckpoints` 增量（memory/compact，静态面）：两 `AtomicLong`。
  - `saves`：save 入口计数；`rollbacks`：rollback 入口计数（三档合并单桶）。
- 嵌套 `record CheckpointStats(long saves, long rollbacks)` + `stats()` + `resetForTest()`。

## 兼容性

纯增量读面：save/rollback/latestWindow 返回语义逐位不变；静态面理由同 R46–R114 先例；无新配置项。

## Out of Scope

- 三档细分桶（RollbackLevel 已在 state 值）。
- latestWindow 查询计数（读侧无副作用）。
