# 721 — 嵌入超限分批装饰器

> 来源：G 会话第 45 轮 = effort #721（批量嵌入韧性）/ [T1042](../../.wayfinder/tickets/T1042-chunking-embedding.md) / [T1043](../../.wayfinder/tickets/T1043-chunking-embedding-verify.md) / impl 621。
> **补档注记**：本 spec 文件在 R45 轮漏写（代码/测试/票已入，提交 54a698c 之前后续轮）——收口时补档。

## Problem

EmbeddingModel 消费方（语义缓存 55/skill 排序 605）单条 embed 为主；批量嵌入场景（首次建缓存/批量导入）单请求输入数组超供应商 cap（如 OpenAI 2048）即 400——无切分面。

## Solution

`ChunkingEmbeddingModel`（resilience/cache，实现 EmbeddingModel 装饰器）：仅覆写 `call(EmbeddingRequest)`——instructions 超 maxBatchSize 切块顺序调用 delegate、输出全局 index 重排拼接；≤max 直通零拷贝；maxBatchSize≥1 构造 fail-fast；embed(String/List/Document) default 方法经 call 路由自动受益。纯确定性（无时序窗——并发合并另题）。

## Testing Decisions

5 条 max=2 → 3 次 delegate+顺序与索引正确；≤max 单次直通；构造校验；delegate 异常透传。

## Out of Scope

单条巨型文档（不处理）；usage 合并（分次计费口径）；并发合并窗（时序题）。
