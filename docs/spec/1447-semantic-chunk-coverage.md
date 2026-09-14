# 1447 — 语义切片索引覆盖读面

> 来源：L 会话第 47 轮 = effort #1447（票 T2193 / T2194 / impl 1099）。借鉴：Elasticsearch index stats（分片分布失衡 = 索引策略倾斜的第一信号）。

## Problem Statement

`SemanticChunkIndex`（溢出制品语义切片索引）只有 index/locate 执行面：**索引覆盖**（多少制品已入索引、切片总数、单制品切片数失衡）无读面——分块策略倾斜（某制品切片爆炸占满索引）静默。

## 目标

- `SemanticChunkIndex`（spill）增量：
  - `coverageStats()` → `record CoverageStats(indexedUries, totalChunks, maxChunksPerUri, largestUri)`；
  - 覆盖口径：已索引 uri 数 / 切片总数 / 单 uri 最大切片数与最厚制品定位（切片失衡信号）；
  - 空索引零哨兵（largestUri=null）；provider 不可用时 index 短路——覆盖保持空。
- 实例面增量（既有实例可读），index/locate 语义逐位不变。

## 兼容性

纯增量读面：byUri 状态只读遍历，无锁需求变更（调用方线程约定不变）。

## Out of Scope

- 切片质量评分（excerpt 语义域）。
- 增量索引/失效剔除策略（既有 byUri put 覆盖语义不变）。
- 按会话分桶（基数红线）。
