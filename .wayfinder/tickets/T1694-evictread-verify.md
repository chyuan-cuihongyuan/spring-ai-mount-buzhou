---
id: T1694
title: evict×readRange 逐出复活组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1693
created: 2026-09-15
---

## Question

J 会话第 117 轮：逐出复活组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（EvictReadBackComboTest，HandleLifecycleRegistry 骨架）：逐出→回读→再逐出链双 stats 一致 + 守恒。定向 `mvn -pl buzhou-spill -am test -Dtest='EvictReadBackComboTest'` 绿。
