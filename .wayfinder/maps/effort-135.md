# Wayfinder Map — Buzhou 文件咨询锁（effort #135，A 会话第 30 轮）

> A 侧票号 T501+ / spec 偶数段沿用。借鉴 ShedLock（多实例单跑）。

## Destination

锁文件原子抢锁 + 仅持有者释放 + 陈旧回收——定时任务多实例各跑一份问题的
通用底座（spec 127/162 fog 的「多实例节流」由此起步）。

## Notes

- 诚实口径：咨询锁防「同刻双跑」不防「持锁僵死」（僵死走 stale 判定 +
  强制回收；持有者存活不校验——进程心跳另一族）；网络 FS 原子性以平台为准。

## Decisions so far

- [AdvisoryFileLock](../tickets/T540-file-lock.md) — tryAcquire/release/owner/
  stale/forceRelease。

## Not yet specified

- ArchivePurgeJob/巡检犬接锁（单实例执行档）；租约自动续期。

## Out of scope

- 数据库/Redis 锁后端；持有者存活心跳校验。

## Tickets

- [x] [T540 文件咨询锁](../tickets/T540-file-lock.md)（impl-302）
- [x] [T541 收口提交](../tickets/T541-file-lock-close.md)（impl-302）
