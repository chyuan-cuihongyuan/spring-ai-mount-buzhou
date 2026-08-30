# Spec 130 — 归档定时清理（effort #91）

> wayfinder map：`.wayfinder92/MAP.md`（T455–T456，A 会话 #92）。spec 103 fog「归档
> autoconfig 定时」收口。借鉴：S3 lifecycle 触发（到期规则自动兑现）。

## Problem Statement

`SessionArchiver.purgeExpired(ttl)` 只有手动面（spec 103 落地时明确「不自装
调度」）——宿主要自己挂 cron，多数部署根本不挂：归档冷层只增不减，TTL 治理
名存实亡。

## Solution

`retention/ArchivePurgeJob`（SmartLifecycle，RetentionSweeper 同骨架）：单线程
`scheduleWithFixedDelay` 按 TTL 调 `purgeExpired`；`purgeOnce()` 手动面恒可用；
每轮删除数经 listener 可观测（0 也通知——「跑过但无事可做」是事实）；单轮异常
只记 ERROR 不杀调度线程。autoconfig 接线：`buzhou.session-archive.purge-enabled`
（默认<b>关</b>——删除动作必须显式开启）+ `purge-ttl`（默认 7d）+
`purge-interval`（默认 1h）；另补 `SessionArchiver` 兜底 bean
（@ConditionalOnMissingBean）。诚实边界：单进程调度，多实例各跑一份
（purgeExpired 幂等无害但非零成本——多实例节流是后续 fog）。

## User Stories

1. 作为运维，我开一个开关就得到归档 TTL 的自动兑现，所以冷层不再只增不减，
   且每轮删除数可观测。
2. 作为宿主开发者，默认关 + purgeOnce 手动面，所以既有部署零行为变化、测试
   里可确定性驱动。

## Testing Decisions

- 红队：默认关不排程但手动面可用（ttl=0 全清语义透传）；开启后 40ms 周期触发
  ≥2 轮 + 未到期不删 + stop 后不再触发；构造参数 fail-fast（null/零 interval
  仅 enabled 时校验）。BuzhouStartupValidationTest 回归兜 autoconfig 面。

## Out of Scope

- 多实例节流（选主/分布式锁）；purge 审计事件；cron 表达式；归档搬移。

## Further Notes

- 与 spec 121/124 同纪律：窗口与治理动作由运维 cron 或本 job 驱动，不自装
  第二套调度语义。
