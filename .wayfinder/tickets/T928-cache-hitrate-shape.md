---
id: T928
title: 缓存命中率便利 getter 的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

Response/Semantic 两缓存 store 只有 hit/miss 计数——命中率要调用方自算。加便利面吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 40 轮 = effort #600 / spec 639 / impl 492）：两 store 各加 `hitRate()`（0..1；零请求 = 0.0 诚实口径——与 PromptPrefixCache.Stats.hitRate 同款语义）。
