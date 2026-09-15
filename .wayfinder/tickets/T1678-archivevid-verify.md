---
id: T1678
title: 归档×evidence 回查联动组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1677
created: 2026-09-15
---

## Question

J 会话第 109 轮：归档回查联动如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ArchiveEvidenceComboTest）：归档后回查 + 双读面守恒 + reset。定向 `mvn -pl buzhou-core -am test -Dtest='ArchiveEvidenceComboTest'` 绿。
