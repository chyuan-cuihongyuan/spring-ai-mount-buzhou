---
id: T1602
title: evidence_lookup 证据回查读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1601
created: 2026-09-15
---

## Question

J 会话第 73 轮：EvidenceLookupStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（EvidenceLookupStatsTest，InMemory MessageStore 骨架——见既有 evidence 测试）：命中全文 → completeReads=1；带 limit 截断 → slicedReads=1；未知 id → misses=1；双守恒恒等式；resetForTest 归零。定向 `mvn -pl buzhou-memory -am test -Dtest='EvidenceLookupStatsTest'` 绿 + 既有回查回归绿。
