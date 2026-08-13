# 15 — memory · 向量 recall 四模搜（pgvector 单库）

**What to build:** 压缩后「之前哪段讲过 X」可回答：text/embedding/time/hybrid 四种模式模糊召回精确原文，向量索引与消息台账单库同事务，含 embedding provider 抽象（spill 语义回读复用）。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] `RecallSearchQuery{mode: TEXT|EMBEDDING|TIME|HYBRID, query, start/end, after/before sequenceId, limit}`
- [ ] JDBC 后端：消息台账 + pgvector 列 + HNSW 索引，**单库同事务双写**（不引入独立向量库）；hybrid=RRF 融合可调权
- [ ] InMemory 后端降级：TEXT/TIME 可用、EMBEDDING/HYBRID 禁用或降级（行为明确非静默）
- [ ] sequence 游标分页；过滤工具消息与检索自身消息防递归自指
- [ ] embedding provider 抽象（模型切换/dimension 可配）
- [ ] 与 `EvidenceLookupTool` 互补分工明确（模糊召回 vs 精确指针）
- [ ] store 契约测试 + 端到端召回断言
- [ ] spec 01（记忆压缩）同步

> spec 12 §memory-13；[T41](../tickets/T41-memory-vector-recall-search.md)。源：letta 24,230★（conversation_search 四模式）。
