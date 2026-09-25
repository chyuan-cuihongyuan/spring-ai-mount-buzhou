---
id: T6229
title: T 会话 T15 Simple8b 位打包的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

小整数列怎么字对齐多值共居？（spec 6015 /
effort #6015 / T15）

## Resolution

**Simple8b（core/message）**：4 位选择子+60 位载荷 16 档
（240×0 至 1×60）贪心选档；decode 尾部截断原值数；
wordCount/valueCount 读数；null/负值/≥2^60 fail-fast。
