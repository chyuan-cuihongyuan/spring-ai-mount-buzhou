---
id: U7212
title: U 会话 U6 周期对账的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7211]
created: 2026-09-29
---

## Question

U6 合同怎么逐一验绿？（spec 7005 / effort #7005 / U6）

## Resolution

**验证通过**：快照 diff 仅 +4；全仓 mvn verify BUILD
SUCCESS 三门绿；USession7000LedgerAuditTest 台账核账全绿。
