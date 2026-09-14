---
id: T1672
title: cipher×readBack 组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1671
created: 2026-09-15
---

## Question

J 会话第 106 轮：加密回读组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CipherReadBackComboTest，加密 DiskSpillStore 骨架）：回读密文 → decryptCalls≥1 + ReadRangeStats 守恒。定向 `mvn -pl buzhou-spill -am test -Dtest='CipherReadBackComboTest'` 绿。
