# Spec 1709 — 会话迁移结果普查（effort #1709，R10）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2619–T2620，impl 1309）。借鉴：
> Kafka 分区再均衡的过程结果普查——搬「对了没」已有对账（MigrationReconciliation
> spec 825），搬「成了没/为何没搬」缺位。

## Problem Statement

SessionMigrator 迁移批次跑完只有数据对账面：多少会话真搬了/多少因「已在
目标侧」跳过/多少源为空跳过/多少失败——过程无普查，容量规划与故障归因
（迁移失败 vs 从未尝试）两头抓瞎。

## Solution

`MigrationOutcomeStats`（core/session，实例面线程安全）：`Outcome` 闭集
（MIGRATED/SKIPPED_CURRENT/SKIPPED_EMPTY/FAILED）+ `record(outcome)` +
`census()` → `MigrationCensus(total/migrated/skippedCurrent/skippedEmpty/
failed/attemptSuccessRatio)`。成功率分母 = migrated+failed（跳过非尝试）；
无尝试哨兵 −1；`resetForTest()` house 惯例。

## User Stories

1. 作为平台运维，failed=0 但 skippedCurrent=90% → 迁移任务在空转，查调度去重。
2. 作为平台运维，attemptSuccessRatio=0.6 → 迁移器本身有问题，对账面不用看了。

## Implementation Decisions

- EnumMap+AtomicLong 四桶；成功/失败/跳过语义闭集，不猜迁移器内部。
- 纯读面 opt-in：宿主包装迁移调用逐笔记。

## Testing Decisions

- 空普查处方哨兵 −1；五笔混合归账 + 成功率 2/3；resetForTest 归零。

## Out of Scope

- 不改 SessionMigrator；不做对账（数据维度归 spec 825）。

## Further Notes

- 迁移双面：过程普查（本轮）+ 数据对账（825）——Kafka 再均衡的完整镜像。
