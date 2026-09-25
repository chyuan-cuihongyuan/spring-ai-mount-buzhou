---
id: T6227
title: T 会话 T14 Gorilla XOR 浮点压缩的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

时序浮点流怎么位级无损高压缩？（spec 6014 /
effort #6014 / T14）

## Resolution

**GorillaXor（core/message）**：相邻 double XOR 三态编码
（同值 0 位/窗复用 '10'/新窗 '11'+5+6 位）；decompress
按位无损（rawBits 保 NaN 载荷与 ±0）；compressedBits/
originalBits 读数；null fail-fast。
