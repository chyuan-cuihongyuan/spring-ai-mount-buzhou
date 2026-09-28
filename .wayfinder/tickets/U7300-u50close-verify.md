---
id: U7300
title: U 会话 U50 收口对账的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7299]
created: 2026-09-29
---

## Question

U50 合同怎么逐一验绿？（spec 7049 / effort #7049 / U50）

## Resolution

**验证通过**：快照 diff 仅 +1；全仓 mvn verify BUILD SUCCESS 三门绿；USession7000LedgerAuditTest 全绿；origin/main 推送封卷。
