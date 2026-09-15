---
id: T1682
title: evidence×episodic 独立性组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1681
created: 2026-09-15
---

## Question

J 会话第 111 轮：独立性组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（EvidenceEpisodicComboTest，InMemory stores 骨架）：交叉调用后双读面互不串账 + reset 独立。定向 `mvn -pl buzhou-memory -am test -Dtest='EvidenceEpisodicComboTest'` 绿。
