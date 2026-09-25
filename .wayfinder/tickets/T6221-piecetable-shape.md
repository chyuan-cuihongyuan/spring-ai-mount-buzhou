---
id: T6221
title: T 会话 T11 Piece Table 文本缓冲的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

编辑器缓冲怎么编辑不复制原文？（spec 6010 /
effort #6010 / T11）

## Resolution

**PieceTable（core/fs）**：原稿只读+追加型增量缓冲——片段表
按序拼装视图，insert/delete 只增删/切分片段不搬原文；
text()/length/pieceCount/addedLength 读数（增量可见）；
越界与畸形 fail-fast。
