# 1011 — spill 容量水位读面

> 来源：J 会话第 12 轮 = effort #1011（[T1473](../../.wayfinder/tickets/T1473-spill-usage-shape.md) / [T1474](../../.wayfinder/tickets/T1474-spill-usage-verify.md) / impl 764）。借鉴：Redis [INFO memory](https://redis.io/docs/latest/commands/info/) / PostgreSQL `pg_database_size()`（存储占用是一等运维读数）。

## Problem Statement

`DiskSpillStore` 的 `totalSpillBytes()` 是私有方法（仅配额守卫内部比较用）：磁盘占用与 spill 条目数对外不可读——「离 maxTotalBytes 还有多远」「会话扇出产生多少 spill」运维不可见；`QuotaExceededException` 拒绝事后无水位证据可查。

## 目标

- 新公共 record `SpillUsage(long totalBytes, int entryCount)`（spill 包，api 面）。
- `DiskSpillStore.usage()`：公开快照（synchronized 与写路径同锁；一次目录 walk 同时计字节与条数——与配额守卫 `totalSpillBytes()` 同口径）。

## 兼容性

纯增量读面：store/load/delete/配额语义零变化；无新配置项。

## Out of Scope

- 按会话分桶占用（会话目录已天然分桶，`sweepOrphans` 口径可查——不重复设面）。
- 配额上限入快照（调用方自持配置，诚实不重复事实）。
