# Wayfinder Map — Buzhou 巡检犬接锁（effort #137，A 会话第 32 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 182 fog「任务接锁」第二站。

## Destination

TurnStallWatchdog 5 参带锁构造：未获锁零通知（null 与空表语义区分）+
skippedForLock 计数；用后即还；无锁零变化。

## Decisions so far

- [巡检犬接锁](../tickets/T546-watchdog-lock.md) — null 哨兵 + 计数面。

## Not yet specified

- autoconfig 锁路径键（两任务共用一把或分锁）。

## Out of scope

- 续期；心跳表跨实例（B 侧舱表族正交）。

## Tickets

- [x] [T546 巡检犬接锁](../tickets/T546-watchdog-lock.md)（impl-304）
- [x] [T547 收口提交](../tickets/T547-watchdog-lock-close.md)（impl-304）
