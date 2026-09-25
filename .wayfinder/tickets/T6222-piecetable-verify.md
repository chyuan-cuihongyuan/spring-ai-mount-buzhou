---
id: T6222
title: T 会话 T11 Piece Table 文本缓冲的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6221]
created: 2026-09-26
---

## Question

T11 合同怎么逐一验绿？（spec 6010 / effort #6010 / T11）

## Resolution

**验证通过**：PieceTableTest 五测全绿——500 混合编辑
StringBuilder oracle 全等；尾插/中点切分/跨片段删除钉住；
addedLength=累计插入数；pieceCount 有界；fail-fast。
