---
id: T1606
title: 会话归档操作读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1605
created: 2026-09-15
---

## Question

J 会话第 75 轮：ArchiveStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ArchiveStatsTest，BuzhouStores 骨架——见既有 SessionArchiver 测试）：有内容会话归档 → archived=1；空会话 → emptySkipped=1；pdb 下限拒 → pdbRejected=1（如可注入）；resetForTest 归零。定向 `mvn -pl buzhou-core -am test -Dtest='ArchiveStatsTest'` 绿 + 既有 SessionArchiver 回归绿。
