# Wayfinder Map — Buzhou 归档清理接锁（effort #136，A 会话第 31 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 182 fog「任务接锁」首站。

## Destination

ArchivePurgeJob 带锁构造：每轮先抢咨询锁，未获锁跳过（SKIPPED_LOCKED=-1
通知——「别的实例在跑」也是事实）；用后即还；无锁构造零变化。

## Notes

- 抢锁 IO 失败按未获锁跳过（fail-safe 不放大为清理故障）；释放失败 WARN
  （下轮按陈旧回收）。

## Decisions so far

- [接锁档](../tickets/T543-purge-lock.md) — 5 参构造 + finally 释放 +
  SKIPPED_LOCKED 哨兵。

## Not yet specified

- 巡检犬接锁同款；autoconfig yml 键（lock-path）。

## Out of scope

- 锁自动续期；DB/Redis 后端。

## Tickets

- [x] [T543 清理接锁](../tickets/T543-purge-lock.md)（impl-303）
- [x] [T544 收口提交](../tickets/T544-purge-lock-close.md)（impl-303）
