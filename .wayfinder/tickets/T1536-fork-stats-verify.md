---
id: T1536
title: time-travel fork 操作计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1535
created: 2026-09-14
---

## Question

J 会话第 41 轮：fork 操作计数读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ForkStatsTest，InMemory MessageStore + 多轮 user 消息骨架）：fork 两次 → forksCreated=2；messagesCopied 累计；fresh 零值；forkFrom 返回的新 id 行为回归（原会话不动）。定向 `mvn -pl buzhou-memory test -Dtest='ForkStatsTest'` 绿 + 既有 fork 回归绿。
