---
id: T6201
title: T 会话 T1 对账门落位的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-25
---

## Question

T 系（6000 段）开卷怎么预防号段漂移？（spec 6000 /
effort #6000 / T1）

## Resolution

**TSession6000LedgerAuditTest（starter）**：S 系公式族第七
应用——spec 6000–6049 自扩展扫描 ↔ shape/verify 票对
（6201+2(N−6000)）↔ impl（2201+N−6000）↔ README 行，四面
互证 + 严格递增断言。
