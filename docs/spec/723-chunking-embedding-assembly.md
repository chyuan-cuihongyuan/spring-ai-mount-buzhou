# 723 — 分批嵌入 yml 装配接线

> 来源：G 会话第 24 轮 = effort #723（721 装配兑现）/ [T1046](../../.wayfinder/tickets/T1046-chunking-assembly.md) / [T1047](../../.wayfinder/tickets/T1047-chunking-assembly-verify.md) / impl 623。
> **补档注记**：本 spec 文件在 R24 轮漏写——收口时补档。

## Problem

721 ChunkingEmbeddingModel 是原语——ResilienceModule.configure 的 semanticEmbeddingModel 未包装，yml 无法启用。

## Solution

`SemanticCache` record 扩第 6 组件 `embeddingMaxBatch`（Integer 默认 0=关；>0 时 semanticEmbeddingModel 包 ChunkingEmbeddingModel(model, batch)）+5 参兼容构造+@ConstructorBinding 规范构造（R39/R48 坑位规避）；yml `buzhou.resilience.semantic-cache.embedding-max-batch` + metadata；装配点=ResilienceModule semanticCacheStore 创建处（embeddingModel 局部变量包装）。

## Testing Decisions

record 绑定（三键启用）+默认关语义+负值 fail-fast+metadata 已知键宇宙。

## Out of Scope

只挂语义缓存路径（skills 排序等其他 EmbeddingModel 消费点宿主自包）；fail 场景沿用既有 fail-fast。
