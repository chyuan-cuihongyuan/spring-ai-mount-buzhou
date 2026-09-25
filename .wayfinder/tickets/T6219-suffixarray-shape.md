---
id: T6219
title: T 会话 T10 Suffix Array 后缀数组的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

静态文本子串查询怎么一次建索引？（spec 6009 /
effort #6009 / T10）

## Resolution

**SuffixArray（core/metrics）**：Manber-Myer 倍增构造 +
Kasai LCP；contains/occurrenceCount 二分后缀序（重叠全计）；
suffixArray/lcpArray 防御性副本读数；null/空文本与 null/
空查询 fail-fast。
