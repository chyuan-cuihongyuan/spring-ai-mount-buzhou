---
id: T1046
title: 分批嵌入 yml 装配接线的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

721 原语无装配——yml 怎么开？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 24 轮 = effort #723 / spec 723 / impl 623）：SemanticCache 扩 embeddingMaxBatch（默认 0=关，6 组件规范构造+@ConstructorBinding+5 参兼容——R39/R48 坑规避）；ResilienceModule 在语义缓存装配点包装 ChunkingEmbeddingModel；yml semantic-cache.embedding-max-batch+metadata。只挂语义缓存路径（其余消费点宿主自包）。
