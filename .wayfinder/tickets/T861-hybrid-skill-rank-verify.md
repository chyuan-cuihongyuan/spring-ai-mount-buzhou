---
id: T861
title: 混合排序验证口径（RRF 分歧/加权翻转/降级/分词边界）
type: task
status: closed
assignee: zcode-f
blocked-by: T860
created: 2026-09-12
---

## Question

混合排序语义如何钉住不回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（HybridSkillRankerTest 6/6，buzhou-skills 96/96）：

- 词法：精确错误码命中第一；CJK bigram 中文命中；tf 饱和（keyword×3 > alpha×1）定词法序。
- RRF：语义序 [a,b,c] × 词法序 [b,a,c] 等权 → 并列保原序；词法 3:1 加权 → 翻转为 b。
- 语义失败（stub 嵌入抛）→ 纯词法 + semanticFallbackCount=1。
- tokenize：ASCII 小写化 / CJK 分段不跨界（袋词序无关）/ null 与空串安全。
- 守卫：单候选与空白 hint 原样返回；null ranker / 零权重构造拒绝。
