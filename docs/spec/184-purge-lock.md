# Spec 184 — 归档清理接锁（effort #136）

> wayfinder map：`.wayfinder/maps/effort-136.md`（T543–T544）。spec 182（咨询锁）的
> 首个接线站；spec 127 fog「多实例节流」落地。

## Problem Statement

ArchivePurgeJob 多实例各跑一份（spec 127 诚实入档的非零成本）——咨询锁
（spec 182）已就位但没人用。

## Solution

`ArchivePurgeJob` 5 参构造（可选 `AdvisoryFileLock`，null = 既有零变化）：
每轮 `tryAcquire`（持有者 = 进程标识派生）；未获锁跳过本轮——返回并通知
`SKIPPED_LOCKED = -1`（「别的实例在跑」也是事实），archiver 零调用；执行完
`finally release`（用后即还）。抢锁 IO 失败按未获锁跳过（fail-safe 不放大为
清理故障）；释放失败只 WARN（残留靠陈旧回收）。

## User Stories

1. 作为运维，多实例部署配一个锁路径，定时清理即单实例执行——重复成本与
   日志噪音消失。

## Testing Decisions

- 红队：他持锁跳过（-1 + archiver 零调用）→ 释放后正常清理（1）→ 用后即还
  （再跑空转 0）。清理/锁既有回归。

## Out of Scope

- 巡检犬接锁；autoconfig 锁路径键；续期。

## Further Notes

- 哨兵语义与「0 也通知」一致：-1 = 本实例没跑（别的实例在跑）。
