# Spec 5028 — Segment Log 分段日志（effort #5028，S29）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6157–T6158，impl 2179）。
> 借鉴：Kafka 分段日志（segment roll + 最旧段保留淘汰）。

## Problem Statement

追加日志的病：单文件无限增长（句柄大、清理只能整文件）或
无保留上限（磁盘耗尽）——**容量滚动 + 最旧段淘汰面**缺失。

## Solution

`SegmentLog`（core/recovery）：

- `append(record)`：分配递增 LSN；段满（segmentCapacity 条）
  **滚动新段**；段数超 maxSegments → 淘汰最旧段（其记录数
  计入 droppedCount——保留上限诚实可见）；
- `readAll()`：存活记录按 LSN 序（跨段拼接）；
- 读数：segmentCount/totalAppended/droppedCount/
  firstSurvivingLsn；
- fail-fast：容量参数 ≤0、null/空 record。

## User Stories

1. 作为日志作者，句柄小、清理按段——保留上限防磁盘耗尽。
2. 作为审计作者，同追加序列同存活窗口（确定性可回放）。

## Testing Decisions

- 段满滚动（cap=3 灌 4 条 → 2 段）；超上限逐最旧段
 （dropped 计数=被逐记录数）；readAll 跨段按序；firstSurviving
  读数；畸形参数/记录 fail-fast；确定性回放。

## Out of Scope

- 不做真实文件 IO（本件是分段保留语义面）；不做索引联动
 （SparseIndex 已覆盖查找面）；不做压缩段。

## Further Notes

- 与 SessionArchiver（归档）同族不同面：归档导出 vs 分段
  保留窗口。Wave 5 第五件。
- 里程碑：S29/50（58%）。
