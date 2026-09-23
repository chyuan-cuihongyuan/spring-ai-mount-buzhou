---
id: T6013
title: R 会话 R7 Huffman 前缀码的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

倾斜符号流的最优前缀码怎么免树传输？（spec 4006 / effort #4006 / R7）

## Resolution

**HuffmanCodec（core/message）**：Huffman 1952 + deflate 规范码——
堆合并取码长、码字按 (码长,符号) 字典序推导（码本只存码长），
位打包 MSB 先 + 逐位对表解码；单符号退化 1 位、树深 >57 fail-fast。
与 EliasGamma/Varint 成编码三档。
