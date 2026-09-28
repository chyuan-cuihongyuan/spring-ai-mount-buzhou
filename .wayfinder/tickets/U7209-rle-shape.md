---
id: U7209
title: U 会话 U5 RunLengthCodec 行程编码的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

高重复列怎么折叠存储？（spec 7004 / effort #7004 / U5）

## Resolution

**RunLengthCodec（core/message）**：(count,value) 对
折叠 count∈[1,255] 超长切段；奇长/零计数 fail-fast；
pairCount 压缩比面。
