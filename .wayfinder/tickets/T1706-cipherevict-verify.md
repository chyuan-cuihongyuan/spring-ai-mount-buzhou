---
id: T1706
title: 加密溢出×逐出组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1705
created: 2026-09-16
---

## Question

J 会话第 123 轮：组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CipherEvictComboTest，加密 DiskSpillStore 骨架）：溢出→逐出链双 stats 各自计数一致 + 守恒。定向 `mvn -pl buzhou-spill -am test -Dtest='CipherEvictComboTest'` 绿。
