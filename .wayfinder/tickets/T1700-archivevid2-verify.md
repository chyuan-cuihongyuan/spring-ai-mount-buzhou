---
id: T1700
title: 归档×readRange 生命周期组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1699
created: 2026-09-15
---

## Question

J 会话第 119 轮：组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ArchiveReadRangeComboTest，SpillModule+BuzhouStores 骨架）：溢出→归档→回读链 + ReadRangeStats 守恒。定向 `mvn -pl buzhou-memory -am test -Dtest='ArchiveReadRangeComboTest'` 绿。
