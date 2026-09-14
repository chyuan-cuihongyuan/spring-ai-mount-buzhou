---
id: T1654
title: offload×cipher 加密联动组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1653
created: 2026-09-15
---

## Question

J 会话第 97 轮：加密联动如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（OffloadCipherComboTest，加密 DiskSpillStore 骨架）：offload 后 encryptCalls≥1 + SpillOffloadStats 守恒。定向 `mvn -pl buzhou-spill -am test -Dtest='OffloadCipherComboTest'` 绿。
