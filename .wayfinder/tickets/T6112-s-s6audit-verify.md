---
id: T6112
title: S 会话 S6 周期对账的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6111]
created: 2026-09-24
---

## Question

S6 对账怎么验绿？（spec 5005 / effort #5005 / S6）

## Resolution

**验证通过**：快照再生 +4（reactor classpath）→ 门测试绿；
全仓 16 模块 `mvn verify` BUILD SUCCESS（退出码 0）；
SSession5000LedgerAuditTest 四面互证绿（spec 5000–5004
五轮四件套齐整）。
