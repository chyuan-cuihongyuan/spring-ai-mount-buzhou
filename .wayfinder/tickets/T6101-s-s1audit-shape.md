---
id: T6101
title: S 会话 S1 对账门落位的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

S 系（5000 段）开卷怎么预防号段漂移？（spec 5000 /
effort #5000 / S1）

## Resolution

**SSession5000LedgerAuditTest（starter）**：R 系公式族第六
应用——spec 5000–5049 自扩展扫描 ↔ shape/verify 票对
（6101+2(N−5000)）↔ impl（2151+N−5000）↔ README 行，四面
互证 + 严格递增断言。
