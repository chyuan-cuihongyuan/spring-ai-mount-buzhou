---
id: T6017
title: R 会话 R9 增量基准帧的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

单调（近单调）整数列「绝对值大差值小」怎么压？（spec 4008 / effort #4008 / R9）

## Resolution

**DeltaFrameOfReference（core/message）**：Parquet DELTA 同款——
每帧基准原样 + 帧内增量，全走 zigzag varint（复用 VarintCodec）；
帧界即重置点（跳变只付一次基准）；count 驱动解码，截断/残留
fail-fast；frameSize=1 全基准退化档。
