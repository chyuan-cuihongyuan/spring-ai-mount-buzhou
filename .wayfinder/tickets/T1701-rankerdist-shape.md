---
id: T1701
title: SemanticSkillRanker 排序分布深化（三计数扩展）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1687
created: 2026-09-15
---

## Question

J 会话第 121 轮：skills 排序面深化的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SemanticSkillRanker 仅有 bypassCount 单计数——排序成功次数与跳过（candidates≤1/空 hint）分布不可见。

形状裁决：追加静态两计数 rankCalls/skippedTrivial（candidates≤1 或空 hint 短路径）+ bypassCount 语义不变；stats() 快照含三计数。实例字段不迁移（bypassCount 既有公共 API 保持）。零生产行为改动。
