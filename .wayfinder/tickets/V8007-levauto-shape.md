---
id: V8007
title: V 会话 V4 LevenshteinAutomaton 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

海量候选的 k-容差匹配怎么模式侧自动化？（spec 8003 / effort #8003 / V4）

## Resolution

**LevenshteinAutomaton（core/metrics）**：of(pattern,maxEdits)
+matches 逐字符活性状态行推进（三源取小），终态 ≤k 接受，
行最小值越界早停诚实缺省。
