---
id: T5079
title: Q 会话 R40 varint 的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

字节流整数怎么紧凑且有符号不顶格？（spec 3039 / effort #3039 / R40）

## Resolution

**VarintCodec（core/message，纯函数）**：zigzag 有符号→无符号交
错映射（负小值 1 字节——直接 LEB128 负数 10 字节顶格病的根治）
+LEB128 7 位/字节续位自界定+decodeAt 游标流式推进+截断/超宽
fail-fast。与 EliasGamma（位级）成编码双件按介质粒度选型。
