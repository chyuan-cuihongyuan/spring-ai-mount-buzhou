---
id: T1632
title: memory 双台账组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1631
created: 2026-09-15
---

## Question

J 会话第 88 轮：双台账组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（DualLedgerReadoutTest，InMemory stores 骨架）：交叉调用后 FactLedgerStats 与 EpisodicMemoryStats 各自计数互不串账 + reset 独立。定向 `mvn -pl buzhou-memory -am test -Dtest='DualLedgerReadoutTest'` 绿。
