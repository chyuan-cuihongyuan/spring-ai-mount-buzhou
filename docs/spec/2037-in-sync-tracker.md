# Spec 2037 — 同步副本追踪器（effort #2037，R38）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3175–T3176，impl 1588）。
> 借鉴：Kafka ISR（in-sync replicas）——追上时刻判定同步，滞后剔除/追平回归。

## Problem Statement

多消费/多副本成员的「谁还同步」判定：以固定成员清单（注册即永久
同步）则滞后成员误信；以瞬时状态轮询则抖动剔除回归。需要以**最后
追上时刻**为锚的滞后判定 + 收缩事件显形。

## Solution

`InSyncTracker`（core/recovery，synchronized 小临界区）：

- `register(member)`：初始视为同步（lastCaughtUp=0 起算）；非空非重复
  fail-fast；
- `caughtUp(member, now)`：记追上时刻（**不回拨**——迟到旧追平取
  max）；
- `isInSync(member, now)`：距今 < lagThreshold（< 语义——恰界即掉）；
  未注册 false；
- `inSyncSet(now)`：ISR 快照（注册序）；较上次快照**收缩计 shrinkEvent**
  （扩张不计——收缩才是风险信号）；
- 契约：lagThreshold > 0、时刻非负 fail-fast。

## User Stories

1. 作为复制作者，ISR 由追上时刻锚定——滞后成员即刻剔除、追平自动
   回归，无需人工维护清单。
2. 作为 SRE，shrinkEvents 频率 = 下游消费力紧张度显形。

## Testing Decisions

- 界内同步/恰界掉（< 语义）；滞后剔除 + 追平回归（回归不增缩计数）；
  新注册初始同步 + 从未追上即掉；追平不回拨；未注册 false；反复收缩
  累计 2；畸形七型 fail-fast。

## Out of Scope

- 不做最小 ISR 大小仲裁（min.insync.replicas 多数派语义留白）；不接
  选主/复制链（接线归后续轮）。

## Further Notes

- 与后台任务选主（#331）互补：选主定「谁干」，ISR 定「谁跟得上」。
