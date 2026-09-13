# effort #721 — 嵌入超限分批装饰器

- 会话：G 会话 700 系第 22 轮 ｜ spec [721](../../../docs/spec/721-chunking-embedding-model.md) ｜ 票 [T1042](../tickets/T1042-chunking-embedding.md)/[T1043](../tickets/T1043-chunking-embedding-verify.md) ｜ impl621
- 借鉴：OpenAI embeddings 批量上限（openai-python ≈25K star——单请求输入数组有 cap，超限 400）

## 勘察（排重）

- EmbeddingModel 消费方（语义缓存 55/skill 排序 605）单条 embed 为主；文档批量嵌入场景（首次建缓存/批量导入）单请求超供应商 cap 即 400——无切分面。
- grep -i batch embedding：零命中（Batch 类均为无关域）。

## 决定

`ChunkingEmbeddingModel`（resilience/cache，实现 EmbeddingModel 装饰器）：仅覆写 call(EmbeddingRequest)——instructions 超 maxBatchSize 切块顺序调用 delegate、输出按全局 index 重排拼接；≤max 直通零拷贝；maxBatchSize≥1 fail-fast；embed(Document) 等 default 方法天然经 call 路由自动受益。纯确定性（无时序窗口——并发合并是另一题）。

## 测试

5 条 max=2 → 3 次 delegate+顺序与索引正确/≤max 单次直通/构造校验/委托异常透传。

## 诚实边界

顺序切块非自适应（单条超限的巨型文档不处理）；usage 合并不做（provider 口径异——按 chunk 计费口径本就分次）；并发合并（时序窗）另题。
