---
id: T1690
title: 压缩检查点操作读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1689
created: 2026-09-15
---

## Question

J 会话第 115 轮：CheckpointStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CheckpointStatsTest，InMemory stores 骨架——见既有 CompactionCheckpoints 测试）：save → saves=1；rollback → rollbacks=1；守恒无强约束（写读独立）；resetForTest 归零。定向 `mvn -pl buzhou-memory -am test -Dtest='CheckpointStatsTest'` 绿。
