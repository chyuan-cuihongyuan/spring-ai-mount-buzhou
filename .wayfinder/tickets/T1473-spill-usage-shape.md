---
id: T1473
title: spill 容量水位读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 12 轮：spill 容量水位读面（Redis INFO memory / pg_database_size 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 12 轮 = effort #1011 / spec 1011 / impl 764）：缺口成立——DiskSpillStore 的 `totalSpillBytes()` 是**私有**方法（仅配额守卫内部使用），磁盘占用与条目数对外不可读：容量水位（离 maxTotalBytes 还有多远）运维不可见，配额拒绝（QuotaExceededException）事后无据可查。落点 buzhou-spill：新公共 record `SpillUsage(totalBytes, entryCount)` + `DiskSpillStore.usage()` 公开快照（synchronized 与写路径同锁一致；一次 walk 同时计字节与条数）。配额上限不入快照（调用方自持配置——诚实不重复事实）。
