---
id: U7284
title: U 会话 U42 周期对账的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7283]
created: 2026-09-29
---

## Question

U42 合同怎么逐一验绿？（spec 7041 / effort #7041 / U42）

## Resolution

**验证通过**：快照 diff 仅 +5；全仓 mvn verify BUILD SUCCESS 三门绿；USession7000LedgerAuditTest 全绿。
