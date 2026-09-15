---
id: T1674
title: memory 三读面大组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1673
created: 2026-09-15
---

## Question

J 会话第 107 轮：三读面大组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（MemoryTripleReadoutTest，InMemory stores 骨架）：三读面交叉后各自守恒 + 互不串账 + reset 独立。定向 `mvn -pl buzhou-memory -am test -Dtest='MemoryTripleReadoutTest'` 绿。
