---
id: T1630
title: memory 域双工具组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1629
created: 2026-09-15
---

## Question

J 会话第 87 轮：memory 双工具组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（MemoryToolsReadoutTest，InMemory stores 骨架）：交叉调用后 CompactNowStats 与 EpisodicMemoryStats 各自守恒 + reset 独立。定向 `mvn -pl buzhou-memory -am test -Dtest='MemoryToolsReadoutTest'` 绿。
