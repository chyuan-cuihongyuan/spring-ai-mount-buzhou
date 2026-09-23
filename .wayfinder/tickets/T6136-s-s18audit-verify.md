---
id: T6136
title: S 会话 S18 周期对账的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6135]
created: 2026-09-24
---

## Question

S18 对账怎么验绿？（spec 5017 / effort #5017 / S18）

## Resolution

**验证通过**：快照再生 +5（reactor classpath）→ 门测试绿；
全仓 16 模块 `mvn verify` BUILD SUCCESS（退出码 0）；
SSession5000LedgerAuditTest 四面互证绿（spec 5000–5016
十七轮四件套齐整）。
