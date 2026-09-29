---
id: V8008
title: V 会话 V4 LevenshteinAutomaton 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8007]
created: 2026-09-29
---

## Question

V4 合同怎么逐一验绿？（spec 8003 / effort #8003 / V4）

## Resolution

**验证通过**：三测全绿——kitten/2 手锚双例；300 随机 vs
全矩阵 DP 圣像判定全等；fail-fast（null/空模式/负 k）。
