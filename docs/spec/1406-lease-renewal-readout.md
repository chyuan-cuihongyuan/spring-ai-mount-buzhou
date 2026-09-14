# 1406 — 租约续期健康读面

> 来源：L 会话第 7 轮 = effort #1406（票 T2113 / T2114 / impl 1059）。**换题记录**：原题序号 R6「事件序列缺口检测」勘察发现 EventRecord 无序号字段（缺口无源，不伪实现）；R11 TTFT 已有（spec 46/T170）——顺延 R10 备选题。借鉴：Redisson watchdog（lock TTL 自动续期的看门狗健康审计——续期是否按节奏发生、离过期多近才续上）。

## Problem Statement

`SessionLeaseGuard` 后台按 TTL/3 节奏续租（Redisson watchdog 同型），但读面只有 `renewalCount()`：续期失败次数、续期时剩余租期水位、末次续期时刻全不可见。「会话租约为什么丢了」只能是事后追溯——调度饿死（续期线程没跑）/存储抖动（renew 往返失败）在丢租约前无信号。

## 目标

- `SessionLeaseGuard`（core/internal/session）增量：
  - `renewFailures` 计数（renewQuietly/renewOrLose 两失败路径，终态前恰一次）；
  - `minRemainingAtRenewalMillis` 水位：每次成功续期时记录「当时剩余租期」，取最小——调度饿死/存储抖动时水位收窄（近过期续上是前兆信号）；
  - `lastRenewalAtEpochMillis` 末次成功续期时刻；
  - `renewalStats()` → 嵌套 `record RenewalStats(renewals, failures, minRemainingAtRenewalMillis, lastRenewalAtEpochMillis, lost, ttlMillis)`（从未续期哨兵：水位 -1、时刻 0）。
- 续期语义/lost 终态/leak 登记逐位不变（只增记账）。

## 兼容性

internal 包（不入 API 快照面）；`renewalCount()` 保留（既有测试消费）；成功/失败路径语义逐位不变——recordRenewalSuccess 提取公共记账（原先散在两处的 `expiresAt=now+ttl; renewals++`）行为一致。

## Out of Scope

- runtime 级跨会话租约聚合（多会话宿主的汇总面另轮；guard 是 per-session 内部件）。
- 续期时延（renew 往返耗时）分布——延迟归 store 延迟环（spec 810 同族）。
- TTL/3 节奏本身的配置审计（renewThreshold 已有语义）。
