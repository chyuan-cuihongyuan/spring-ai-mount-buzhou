---
id: T1611
title: 归档清理任务读面（PurgeJobStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1607
created: 2026-09-15
---

## Question

J 会话第 79 轮（第 78 轮顺延）：core/retention 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：ArchivePurgeJob.purgeOnce()（归档 TTL 清理周期任务）零计数——清理总量与锁跳过次数不可见（SKIPPED_LOCKED=-1 语义返回无进程内对账）。Quartz/Chron job statistics（周期任务执行/跳过/产出三面）思想。

形状裁决：`ArchivePurgeJob` 内静态 `AtomicLong` 三计数——purgeRounds（purgeOnce 入口）/ purgedTotal（累计清理归档数）/ skippedLocked（分布式锁未获取跳过次数）；嵌套 `record PurgeJobStats` + `stats()` + `resetForTest()`。口径诚实：purgedTotal 为跨轮累计无入口守恒（每轮产出可变）；purgeOnce 返回语义逐位不变。

Out of scope：按 TTL 年龄分布（配置面）；锁等待时长（另轴）。
