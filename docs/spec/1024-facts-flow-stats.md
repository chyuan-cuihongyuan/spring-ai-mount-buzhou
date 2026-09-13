# 1024 — facts 段导入导出行数读面

> 来源：J 会话第 25 轮 = effort #1024（[T1499](../../.wayfinder/tickets/T1499-facts-flow-stats-shape.md) / [T1500](../../.wayfinder/tickets/T1500-facts-flow-stats-verify.md) / impl 777）。借鉴：rsync `--stats`（传输行数/条数是迁移完整性的第一读数）。

## Problem Statement

FactsExporter（spec 36 facts 导出扩展）exportSegment/importSegment 全程零计数：迁移了多少条事实、导入失败几次不可见——facts 体量趋势与迁移完整性（导出行数 vs 导入行数对账）无读数。

## 目标

- `FactsExporter` 增量（buzhou-memory，实例级）：`factsExported`（exportSegment 实际产出行数；空段返回 null 不计）/ `factsImported`（importSegment 成功写入行数）/ `importFailures`（导入异常入桶后照抛——原语义不变）三 AtomicLong。
- 嵌套 record `FactsFlowStats(long factsExported, long factsImported, long importFailures)` + `stats()` 快照。

## 兼容性

纯增量读面：export/import 返回值与异常语义逐位不变；无新配置项。

## Out of Scope

- 跨会话/按 producer 分桶（基数纪律）。
- 导出段体积字节读数（EventPayloadSizeAudit 域）。
