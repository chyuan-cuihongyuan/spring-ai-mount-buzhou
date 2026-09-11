---
id: T872
title: 语义缓存维度漂移的可见面形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

嵌入模型变更（维度 1536→768）后，语义缓存旧条目与查询维度不一致——cosine 防御性跳过（spec 55），但命中面静默塌方，排障只剩「命中率莫名归零」。可见面怎么给？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 12 轮 = effort #600 / spec 611 / impl 464）：

1. `SemanticCacheStore.dimensionMismatches()` 计数器（每查询×每条不匹配条目累计）+ 首次漂移 WARN 一次（AtomicBoolean 去重）：提示嵌入模型可能已变更、建议清缓存。
2. 维度检查前移到 cosine 之前（同语义：跳过不参与相似度、不抛）。
3. 不自动清缓存：新条目自然收敛 + LRU/TTL 淘汰旧条目（自然收敛用例钉住）；自动清除属破坏性动作留运维决策。
