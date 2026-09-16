---
id: T1702
title: SemanticSkillRanker 排序分布深化的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1701
created: 2026-09-15
---

## Question

J 会话第 121 轮：排序分布如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SemanticRankerDistTest，桩 EmbeddingModel 骨架——同 SemanticSkillRanker 测试）：正常排序 → rankCalls=1；单候选跳过 → skippedTrivial=1；嵌入失败 → bypassed 增。定向 `mvn -pl buzhou-skills -am test -Dtest='SemanticRankerDistTest'` 绿。
