---
id: T41
title: memory · 向量 recall 三模搜（pgvector 单库）
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

压缩后原文在持久层，但模型只能靠 evidence-id 精确回读——如何支持模糊召回精确原文？事实源：Letta（24,230★：`conversation_search` 四模式 text（BM25/PG ILIKE 降级）/embedding（ANN）/timestamp（范围倒序）/hybrid（RRF 融合可调权）；双写 SQL 事实源+向量库；分页=sequence_id 游标；**过滤工具消息与检索自身消息防递归自指**）。

## 待定决策（研究推荐已备）

1. `RecallSearchQuery{mode: TEXT|EMBEDDING|TIME|HYBRID, query, start/end, after/before sequenceId, limit}` 落既有消息台账 + pgvector 列 + HNSW 索引——**单库双写、事务内同写避免漂移，不引入独立向量库**——采纳。
2. 与 `EvidenceLookupTool` 互补分工：确定性指针管精确回读、三模搜管模糊召回——采纳。
3. embedding provider 抽象（供 T46 语义回读复用；模型切换/dimension 由先落地者定，fog 跟进）——采纳。
4. InMemory 后端的降级实现（无 pgvector 时 TEXT/TIME 可用、EMBEDDING/HYBRID 降级或禁用）——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §3.2（5–8 天，ROI 中高）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §memory-13**（用户常设授权 2026-08-14 ratify、可推翻）。RecallSearchQuery 四模 pgvector 单库同事务；InMemory 后端降级 TEXT/TIME。
