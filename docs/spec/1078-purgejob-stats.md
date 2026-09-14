# 1078 — 归档清理任务读面

> 来源：J 会话第 78 轮 = effort #1078（[T1611](../../.wayfinder/tickets/T1611-purgejob-stats-shape.md) / [T1612](../../.wayfinder/tickets/T1612-purgejob-stats-verify.md) / impl 830）。借鉴：Quartz/Chron job statistics（周期任务的执行/跳过/产出三面对账）。core/retention 域第二轴（R75 归档前置）。

## Problem Statement

`ArchivePurgeJob.purgeOnce()`（归档 TTL 周期清理）零计数——**清理总量与锁跳过次数不可见**：清理任务是否空转（TTL 内无归档）、分布式锁竞争跳过多不多、累计清掉多少归档——retention 策略健康无对账。

## 目标

- `ArchivePurgeJob` 增量（core/retention，静态面）：三 `AtomicLong`。
  - `purgeRounds`：purgeOnce 入口计数；`purgedTotal`：累计清理归档数（跨轮累计，无入口守恒——每轮产出可变）；
  - `skippedLocked`：分布式锁未获取跳过次数（SKIPPED_LOCKED 路径显形）。
- 嵌套 `record PurgeJobStats(long purgeRounds, long purgedTotal, long skippedLocked)` + `stats()` + `resetForTest()`。

## 兼容性

纯增量读面：purgeOnce 返回语义、锁获取与 TTL 判定逐位不变；静态面理由同 R46–R77 先例；无新配置项。

## Out of Scope

- 按 TTL 年龄分布（配置面）。
- 锁等待时长（另轴）。
