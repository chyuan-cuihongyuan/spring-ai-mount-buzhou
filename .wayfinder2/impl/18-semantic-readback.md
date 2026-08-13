# 18 — spill · 语义回读第 4 模式（locate→fetch 两段式）

**What to build:** 「我的溢出数据哪一段讲了 X」：语义查询定位段落（返回 evidence-id+offset+摘要）、模型再以 byte 模式精读——语义是「定位」、既有三模是「取回」的两段式组合。

**Blocked by:** 15（向量 recall——共用 embedding provider 抽象）

**Status:** ready-for-agent

- [ ] durable/cold 层 offload 时**异步**按既有切片边界 embed（hot-tail 不索引，与两级保留对齐）
- [ ] `mode=semantic`（query, k, minScore 可选, tag/filter）返回 top-k chunk 条目（evidence-id + byte offset + 摘要）
- [ ] 两段式闭环：semantic locate → byte fetch 返回真实切片（端到端断言）
- [ ] 默认关（无 embedding provider 时不启用、行为明确）
- [ ] spec 02（Spill）同步

> spec 12 §spill-18；[T46](../tickets/T46-spill-semantic-readback.md)。源：letta 24,230★（archival 向量检索、片段不 pin 上下文——与 Handle 哲学同构）。
