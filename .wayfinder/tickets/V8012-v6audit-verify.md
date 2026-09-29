---
id: V8012
title: V 会话 V6 周期对账的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8011]
created: 2026-09-29
---

## Question

V6 对账怎么验绿？（spec 8005 / effort #8005 / V6）

## Resolution

**验证通过**：快照门 diff 仅 +4；VSession8000LedgerAuditTest 四测绿
（spec 8000–8004 零缺位）；全仓 16 模块 verify BUILD SUCCESS。
