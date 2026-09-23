---
id: T6160
title: S 会话 S30 周期对账的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6159]
created: 2026-09-24
---

## Question

S30 对账怎么验绿？（spec 5029 / effort #5029 / S30）

## Resolution

**验证通过**：快照再生 +5（reactor classpath）→ 门测试绿；
全仓 16 模块 `mvn verify` BUILD SUCCESS（退出码 0）；
SSession5000LedgerAuditTest 四面互证绿（spec 5000–5028
廿九轮四件套齐整）。
