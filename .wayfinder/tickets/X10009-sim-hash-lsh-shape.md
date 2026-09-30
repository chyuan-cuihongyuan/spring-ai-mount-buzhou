---
id: X10009
title: X 会话 X5 SimHash Lsh 位指纹海明分段索引 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-09-30
---

## Question

形状怎么定？

## Resolution

SimHashLsh（core/concurrent）：blocks ∈ {2,4,8} 分块倒排+验距精确集（Manku 2007 思想——SimHashFingerprint 已占不同面）。
