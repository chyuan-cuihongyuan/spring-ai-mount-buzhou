---
id: T1689
title: 压缩检查点操作读面（CheckpointStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1663
created: 2026-09-15
---

## Question

J 会话第 115 轮：压缩检查点的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：CompactionCheckpoints（压缩前检查点 + 三档回滚）操作零计数——检查点保存量与回滚量是压缩安全网健康信号（数据库 savepoint 使用率思想）。

形状裁决：`CompactionCheckpoints` 内静态 `AtomicLong` 三计数——saves（save 入口）/ rollbacks（rollback 入口）/ rollbacksByLevel 简化为 rollbacks 单桶（三档细分留后续）；嵌套 `record CheckpointStats(saves, rollbacks)` + `stats()` + `resetForTest()`。静态面理由同族先例；save/rollback 返回语义逐位不变。

Out of scope：三档细分桶（RollbackLevel 已在 state 值）；latestWindow 查询计数（读侧无副作用）。
