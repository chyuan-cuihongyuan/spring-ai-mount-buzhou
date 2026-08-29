# Spec 182 — 文件咨询锁（effort #135）

> wayfinder map：`.wayfinder135/MAP.md`（T540–T541）。借鉴：ShedLock
> （定时任务多实例单执行）。

## Problem Statement

ArchivePurgeJob / 停滞巡检犬等自调度任务在多实例部署下每实例各跑一份
（幂等无害但非零成本——spec 127/162 双双记 fog）；缺一个通用的
「多实例单跑」防线。

## Solution

`retention/AdvisoryFileLock`：锁文件以 `createNewFile()` 原子性抢锁（同刻
仅一成功），内容 = 持有者 id + 获取时刻。`release(ownerId)` 仅持有者可释放
（他者释放被拒——防误删）；`owner()` 持有者查询；`stale(now, ttl)` 陈旧判定
（持有超时 = 僵死/崩溃残留）+ `forceRelease()` 处置。诚实口径：咨询锁防
「同刻双跑」不防「持锁僵死」（僵死走陈旧回收；持有者存活不校验——进程
心跳是另一族）；网络 FS 的 createNewFile 原子性以平台语义为准。

## User Stories

1. 作为运维，定时清理/巡检类任务加锁后单实例执行，所以重复跑的成本与
   日志噪音消失。
2. 作为开发者，陈旧回收面让崩溃残留不永久堵死任务（到期可判定可回收）。

## Testing Decisions

- 红队：抢锁互斥 + 仅持有者释放（他者拒）；陈旧按持有时刻 + ttl +
  强制回收幂等；缺锁释放 false + 参数 fail-fast（含 owner 换行拒绝）。

## Out of Scope

- 任务接锁（后续 fog）；租约续期；DB/Redis 后端；心跳校验。

## Further Notes

- 与 SmartLifecycle 任务族（RetentionSweeper/ArchivePurgeJob/巡检犬）正交组合。
