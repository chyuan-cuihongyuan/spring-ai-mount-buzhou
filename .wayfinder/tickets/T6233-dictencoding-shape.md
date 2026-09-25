---
id: T6233
title: T 会话 T17 Dictionary Encoding 字典编码的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

低基数列怎么基数收缩换窄域？（spec 6017 /
effort #6017 / T17）

## Resolution

**DictionaryEncoding（core/message）**：首次出现序字典表+
⌈log₂(去重数)⌉ 位定宽下标列（组合 BitPacking）；单值 0 位
宽；dictionary/distinctCount/indexBitWidth/indicesWordCount
读数；null/空列/越界 fail-fast。
