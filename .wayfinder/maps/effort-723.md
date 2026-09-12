# effort #723 — 分批嵌入 yml 装配接线（721 扩散）

- 会话：G 会话 700 系第 24 轮 ｜ spec [723](../../../docs/spec/723-chunking-embedding-assembly.md) ｜ 票 [T1046](../tickets/T1046-chunking-assembly.md)/[T1047](../tickets/T1047-chunking-assembly-verify.md) ｜ impl623
- 借鉴：—（721 装配兑现）

## 勘察（排重）

- 721 ChunkingEmbeddingModel 是原语——ResilienceModule.configure 的 semanticEmbeddingModel 未包装，yml 无法启用。
- SemanticCache record 5 组件（含 701 maxWeightChars）——再扩 embeddingMaxBatch（默认 0=关）。

## 决定

`SemanticCache` 扩第 6 组件 `embeddingMaxBatch`（Integer 默认 0=关；>0 时 semanticEmbeddingModel 包 ChunkingEmbeddingModel(model, batch)）+5 参兼容构造+@ConstructorBinding 规范构造（R39/R48 坑规避）；yml `buzhou.resilience.semantic-cache.embedding-max-batch`+metadata；装配点=ResilienceModule semanticCacheStore 创建处（embeddingModel 局部变量包装）。

## 测试

yml 启用→store 使用的 embedding 为分批包装（批量写入超限不再炸——经 advisor 级单条路径验证装配存在性以 context hasBean/字段断言）；默认关→原样。

## 诚实边界

只挂语义缓存路径（skills 排序等其他 EmbeddingModel 消费点不追——宿主可自行包装）；fail 场景（enabled 无 EmbeddingModel）沿用既有 fail-fast。
