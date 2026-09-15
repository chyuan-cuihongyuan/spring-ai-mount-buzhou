---
id: T1695
title: SemanticChunkIndex 操作读面（ChunkIndexOpStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1675
created: 2026-09-15
---

## Question

J 会话第 118 轮：语义切片索引的操作读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SemanticChunkIndex.index/locate 零计数——coverageStats（spec 1447）是内容维度（uri/chunk 数），**操作维度**（索引调用/定位查询/无效入参静默跳过）无口径。

形状裁决：`SemanticChunkIndex` 内静态 `AtomicLong` 四计数——indexCalls（index 入口）/ locateCalls（locate 入口）/ skippedInvalid（provider null/uri null/boundaries null/content null 静默跳过显形）/ chunksIndexed（实际入索引切片数）；嵌套 `record ChunkIndexOpStats` + `stats()` + `resetForTest()`。静态面理由同族先例；index/locate 返回语义逐位不变。

Out of scope：按 uri 分桶（coverageStats 已覆盖内容维度）；cosine 分数分布（展示面）。
