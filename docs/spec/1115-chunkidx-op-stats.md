# 1115 — SemanticChunkIndex 操作读面

> 来源：J 会话第 118 轮 = effort #1118（[T1695](../../.wayfinder/tickets/T1695-chunkidx-shape.md) / [T1696](../../.wayfinder/tickets/T1696-chunkidx-verify.md) / impl 866）。借鉴：搜索引擎索引/查询双计数（index vs query 操作分布）。spill 语义索引首轴。

## Problem Statement

`SemanticChunkIndex.index/locate`（语义切片索引双入口）零计数——**操作维度**（索引调用/定位查询/无效入参静默跳过）无口径：coverageStats（spec 1447）是内容维度（uri/chunk 数），操作维度缺失。

## 目标

- `SemanticChunkIndex` 增量（spill，静态面）：四 `AtomicLong`。
  - `indexCalls`：index 入口；`locateCalls`：locate 入口；
  - `skippedInvalid`（provider null/uri null/boundaries null/content null 静默跳过显形）；
  - `chunksIndexed`：实际入索引切片数。
- 嵌套 `record ChunkIndexOpStats(...)` + `stats()` + `resetForTest()`。

## 兼容性

纯增量读面：index/locate 返回语义逐位不变；静态面理由同 R46–R117 先例；无新配置项。

## Out of Scope

- 按 uri 分桶（coverageStats 已覆盖内容维度）。
- cosine 分数分布（展示面）。
