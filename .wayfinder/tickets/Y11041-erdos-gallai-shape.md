---
id: Y11041
title: Y 会话 21 ErdosGallai 图序列可图化 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

ErdosGallai（core/concurrent，静态纯函数面）：isGraphical(degrees)——和偶性+k(k−1)+Σmin(d_i,k) 逐前缀不等式判定；负度 fail-fast；布尔面（奇和非不抛）。
