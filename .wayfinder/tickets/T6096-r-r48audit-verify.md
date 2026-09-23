---
id: T6096
title: R 会话 R48 周期对账的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6095]
created: 2026-09-24
---

## Question

R48 对账怎么验绿？（spec 4047 / effort #4047 / R48）

## Resolution

**验证通过**：快照再生 +5（reactor classpath）→ 门测试绿；
全仓 16 模块 `mvn verify` BUILD SUCCESS（退出码 0）；
RSession4000LedgerAuditTest 四面互证绿（spec 4000–4046
四十轮四件套齐整）。
