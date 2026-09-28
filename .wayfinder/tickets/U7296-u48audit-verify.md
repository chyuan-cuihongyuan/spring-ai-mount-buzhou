---
id: U7296
title: U 会话 U48 周期对账的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7295]
created: 2026-09-29
---

## Question

U48 合同怎么逐一验绿？（spec 7047 / effort #7047 / U48）

## Resolution

**验证通过**：快照 diff 仅 +4；全仓 mvn verify BUILD SUCCESS 三门绿；USession7000LedgerAuditTest 全绿。
