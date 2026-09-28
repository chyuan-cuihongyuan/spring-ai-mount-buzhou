---
id: U7279
title: U 会话 U40 GrayCodeSequence 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

机械计数怎么防多位翻转？（spec 7039 / effort #7039 / U40）

## Resolution

**GrayCodeSequence（core/metrics）**：g(i)=i⊕(i>>1)；decode 逐位前缀异或；n∈[1,30] 全序列；相邻单 bit 性质。
