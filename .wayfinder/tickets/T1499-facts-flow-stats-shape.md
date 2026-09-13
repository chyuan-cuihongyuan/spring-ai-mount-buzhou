---
id: T1499
title: facts 段导入导出行数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 25 轮：facts 段导入导出行数读面（rsync --stats 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 25 轮 = effort #1024 / spec 1024 / impl 777）：缺口成立——FactsExporter（spec 36 导出扩展）exportSegment/importSegment 全程零计数：导出/导入多少条事实、失败多少次不可见——facts 体量与迁移完整性的第一读数。落点 buzhou-memory：实例级 `factsExported`（exportSegment 实际产出的行数；空段返回 null 不计）/ `factsImported`（importSegment 成功写入的行数）/ `importFailures`（导入异常入桶后照抛——原语义不变）三 AtomicLong + 嵌套 record `FactsFlowStats(factsExported, factsImported, importFailures)` + `stats()`。实例级；嵌套类型不动 API 快照。
