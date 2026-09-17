---
id: T5073
title: Q 会话 R37 Elias gamma 的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

幂律整数码流怎么紧凑且流式自界定？（spec 3036 / effort #3036 / R37）

## Resolution

**EliasGammaCodec（core/message，纯函数）**：Elias gamma——k 零+边界
1+k 位余数（小数 1 位起步），零计数自定界免解码表；decode 整串恰
一码字 / decodeAt 游标流式推进双面；bitLength 读数；位级前缀码
与 varint 字节级互补。
