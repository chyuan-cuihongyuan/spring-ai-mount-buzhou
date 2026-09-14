---
id: T1670
title: EpisodeLedger 双实例组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1669
created: 2026-09-15
---

## Question

J 会话第 105 轮：双实例组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（EpisodeLedgerDualInstanceTest，InMemory stores 骨架）：跨实例累计计数正确 + 双守恒 + reset 归零。定向 `mvn -pl buzhou-memory -am test -Dtest='EpisodeLedgerDualInstanceTest'` 绿。
