---
id: T860
title: skill 混合排序的融合算法与分词口径裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

语义排序（spec 59）对精确词（型号/错误码/专有名词）判别弱；hybrid search 的融合应取什么算法与中文分词口径？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 6 轮 = effort #600 / spec 605 / impl 458）：

1. **RRF 融合**（score = Σ w/(k+rank)，k=60 标准值）：免语义 cosine 与 BM25 分数量纲对齐问题；两路加权可配（默认 1:1）。
2. `LexicalSkillRanker`：BM25（k1=1.2/b=0.75 经典值不可配——词法只是混合一路，调参收益低于复杂度）+ 分词 = ASCII 词小写化 + CJK 连续段 bigram（段间不跨界，单字退化 unigram）。
3. `HybridSkillRanker`：语义路嵌入失败（bypassCount 前后差检测）→ 纯词法序 + semanticFallbackCount 观测；并列保原序。
4. opt-in：不接装配，默认排序行为零变化。
