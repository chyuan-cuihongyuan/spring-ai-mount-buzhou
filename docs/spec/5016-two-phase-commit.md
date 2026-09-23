# Spec 5016 — Two-Phase Commit 协调器（effort #5016，S17）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6133–T6134，impl 2167）。
> 借鉴：两阶段提交协议（2PC——prepare 投票 + commit/abort 裁决）。

## Problem Statement

跨资源原子提交的病：各资源自行提交（部分提交——原子性
撕裂）或无阶段约束（ABORTED 后还能 COMMIT——非法迁移
静默通过）——**显式阶段状态机面**缺失。

## Solution

`TwoPhaseCoordinator`（core/transaction）：

- 生命周期：`begin(id, participants)` → PREPARING；
- 投票：`votePrepare(id, participant, yes)`——全部 yes →
  PREPARED；任一 no → ABORTED（一票否决）；
- 裁决：`commit(id)` 仅自 PREPARED → COMMITTED；`abort(id)`
  自 PREPARING/PREPARED → ABORTED；
- 非法迁移 IAE fail-fast（ABORTED 后 commit、COMMITTED 后
  abort、重复投票等）；未知事务/参与者 fail-fast；
- 读数：phaseOf；嵌套 `Phase` 枚举不另立面。

## User Stories

1. 作为跨资源写入作者，全部预备才提交——原子性不撕裂。
2. 作为审计作者，同投票序列同终态（确定性可回放）。

## Testing Decisions

- 全票 PREPARED→COMMITTED；一票否决→ABORTED 且后续
  commit IAE；PREPARING 直接 abort；重复投票/未知参与者/
  非法迁移 fail-fast；确定性回放。

## Out of Scope

- 不做协调者崩溃恢复（pending tx 补账面）；不做三阶段提交
 （3PC）；不做参与者超时器（投票由调用方驱动）。

## Further Notes

- 与 FencingTokenGuard（S5）互补：锁安全守卫 vs 原子提交
  状态机。Wave 3 第六件。
- 里程碑：S17/50（34%）。
