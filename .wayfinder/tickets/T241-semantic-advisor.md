---
Type: task
Status: done
blocked-by: T240-semantic-store.md
---
## Question

SemanticCacheAdvisor（BaseAdvisor，order = 精确缓存 +450 之后 +460）：adviseCall 先精确
（上游已短路）→ 语义查（embed 规范化 query 文本 + cosine 最近邻）命中即重放包装；
未命中透传并在终态聚合后写语义缓存（带 toolCalls 不写——与精确缓存同界）；嵌入调用
异常 = 语义层旁路降级（不阻断主调用，WARN + 计数，诚实：嵌入故障不该弄坏主路径）。
