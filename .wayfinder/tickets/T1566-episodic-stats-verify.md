---
id: T1566
title: 情景记忆读写双守恒读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1565
created: 2026-09-15
---

## Question

J 会话第 55 轮：EpisodicMemoryStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（EpisodicMemoryStatsTest，InMemory SessionStateStore + Stub EmbeddingProvider 骨架——骨架见既有 episodic 测试）：有效 record → recorded=1；null provider 下 record → recordDropped=1；recall 命中（写后召回同 goal）→ recallHits≥1；不相关 goal → recallEmpties=1；双守恒恒等式成立；resetForTest 归零。定向 `mvn -pl buzhou-memory -am test -Dtest='EpisodicMemoryStatsTest'` 绿 + 既有 episodic 回归绿。
