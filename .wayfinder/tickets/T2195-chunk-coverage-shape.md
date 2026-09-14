---
id: T2195
title: 语义切片索引覆盖读面（SemanticChunkIndex 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 47 轮：语义切片索引覆盖读面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：SemanticChunkIndex 只有 index/locate 执行面——覆盖水位（uri 数/切片数/单 uri 峰值）缺失（切片失衡信号）。

形状裁决：实例面增量 coverageStats()→CoverageStats（indexedUries/totalChunks/maxChunksPerUri/largestUri 定位）；空索引零哨兵；provider 不可用短路下覆盖恒空；既有 index 覆盖/locate 语义逐位不变。

Out of scope：切片质量评分；失效剔除策略；会话分桶。
