# Spec 186 — 巡检犬接锁（effort #137）

> wayfinder map：`.wayfinder137/MAP.md`（T546–T547）。spec 182 接线第二站
> （首站 spec 184 清理任务）。

## Problem Statement

停滞巡检犬多实例各跑一份——同一张心跳表每实例只见本地在飞（跨实例心跳是
另一族能力），但巡检告警的重复发送是本实例内的真实噪音；咨询锁就位未接。

## Solution

`TurnStallWatchdog` 5 参构造（可选 `AdvisoryFileLock`）：每轮先抢锁；未获锁
<b>零通知</b>——`inspectOnce` 返回 null（与空表「巡检过没事」语义严格区分：
跳过不是没事），计 `skippedForLock()` + WARN；获锁执行后 finally 释放
（用后即还）。IO 失败按未获锁跳过。null 构造零变化。

## User Stories

1. 作为运维，多实例下告警单实例发——重复告警噪音消失，skippedForLock 是
   「本实例在歇」的证据面。

## Testing Decisions

- 红队：他持锁 null + 零通知 + 计数；释放后正常巡检；用后即还再跑。
  巡检犬既有回归。

## Out of Scope

- autoconfig 锁路径；续期；跨实例心跳。

## Further Notes

- 与 spec 184 对比出两种跳过语义：清理用 -1 哨兵（int 面），巡检用 null
  哨兵（List 面）——「没跑」与「跑过没事」都必须可区分。
