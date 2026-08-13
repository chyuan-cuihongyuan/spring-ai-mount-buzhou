---
id: T46
title: spill · 语义回读第 4 模式（locate→fetch 两段式）
type: task
status: closed
assignee: ""
blocked-by: T41
created: 2026-08-14
---

## Question

「我的数据里哪一段讲了 X」——纯 byte/jsonpath/pagination 无法回答；语义定位如何成为第 4 种回读模式？事实源：Letta（24,230★：archival=通用向量 DB、插入时切块、`archival_memory_search(query,tags,page)` 无阈值参数、片段不能 pin 上下文必须工具查询——**与 Buzhou Handle 哲学同构**）；OpenAI File Search（非 OSS 注记，同向）。

## 待定决策（研究推荐已备）

1. durable/cold 层 offload 时**异步**按既有切片边界 embed（**hot-tail 不索引**，与两级保留对齐）——采纳。
2. `ReadRangeTool` 增 `mode=semantic`（query, k, minScore 可选, tag/filter）返回 top-k chunk 条目（evidence-id + byte offset + 摘要），模型再以 `mode=byte` 精读——**语义是「定位」、byte/jsonpath/pagination 是「取回」，两段式组合而非并列第五种语义**——采纳。
3. **默认关**（embedding provider 依赖 T41 抽象）——采纳。

依据：`docs/research/oss-perfect-tier23.md` §4.4（工作量中偏大，ROI 中高）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §spill-18**（用户常设授权 2026-08-14 ratify、可推翻）。mode=semantic locate→byte fetch 两段式；durable-only 异步索引、默认关。
