---
id: T1696
title: SemanticChunkIndex 操作读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1695
created: 2026-09-15
---

## Question

J 会话第 118 轮：ChunkIndexOpStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ChunkIndexOpStatsTest，桩 EmbeddingProvider 骨架）：有效 index → indexCalls/chunksIndexed 增；provider null → skippedInvalid；locate 有效 → locateCalls；resetForTest 归零。定向 `mvn -pl buzhou-spill -am test -Dtest='ChunkIndexOpStatsTest'` 绿 + 既有 SemanticChunkIndex 回归绿。
