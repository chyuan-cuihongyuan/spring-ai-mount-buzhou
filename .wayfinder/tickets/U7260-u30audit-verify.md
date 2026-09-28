---
id: U7260
title: U 会话 U30 周期对账的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7259]
created: 2026-09-29
---

## Question

U30 合同怎么逐一验绿？（spec 7029 / effort #7029 / U30）

## Resolution

**验证通过**：快照 diff 仅 +5；全仓 mvn verify BUILD SUCCESS 三门绿；USession7000LedgerAuditTest 全绿。
