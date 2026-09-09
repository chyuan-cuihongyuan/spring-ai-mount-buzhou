# Spec 341 — 选主扩散：归档清理与空闲压缩（effort #341）

> wayfinder map：`.wayfinder/maps/effort-341.md`（T673–T674）。C 会话第 42 轮，
> 331 的扩散轮。

## Problem Statement

选主门（331）只覆盖 RetentionSweeper。同类家务任务在多实例部署仍每
实例各自跑：ArchivePurgeJob（归档 TTL 清扫——重复扫归档清单）与
IdleCompactionHousekeeper（空闲会话压缩——两实例同时压缩同一会话
竞写摘要版本）。

## Solution

两任务接同一 LeaderElector 门（331 同纪律）：

- **ArchivePurgeJob**：调度周期先取续——非 leader 跳周期（计数
  `buzhou.archive.skipped-not-leader`）；stop 主动让位；手动
  `purgeOnce()` 不设门。
- **IdleCompactionHousekeeper**：同门同纪律（计数
  `buzhou.idle-compaction.skipped-not-leader`）；手动 `sweepOnce()`
  不设门。
- 构造器加可选 elector 重载（旧构造器委托 null——二进制兼容）；
  装配经 ObjectProvider<LeaderElector> 注入（无 bean 零变化）。
- 三任务共用同一 scope（`buzhou:leader:housekeeping`——331 装配
  既有）：一个 leader 管全部家务。

## User Stories

1. 作为多实例运维，我想归档清扫与空闲压缩也单执行者，所以 归档清单
   不重复扫、压缩不竞写摘要版本。
2. 作为运维，我想手动 purgeOnce/sweepOnce 不受选主限制，所以 紧急
   操作任何实例可执行（331 同纪律）。
3. 作为使用者，我不想配选主时两任务行为与现状完全一致，所以 升级
   零风险。

## Implementation Decisions

- 门判定在各自调度 lambda 内（非 leader/异常都静默跳过留计数）；
  elector 异常吞并 WARN（331 sweepQuietly 同法）。

## Testing Decisions

- 两任务各：非 leader 跳周期留计数 / 手动不设门 / stop 让位
  （stub elector——331 RetentionSweeperLeaderGateTest 同手法）。

## Out of Scope

- 分任务分 scope；任务间调度协同（各自 SmartLifecycle 独立）。

## Further Notes

- 无新公共类型（构造器重载）——快照零 diff 预判。
