---
id: T6302
title: T 会话 T50 收口对账的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6301]
created: 2026-09-28
---

## Question

T50 合同怎么逐一验绿？（spec 6050 / effort #6050 / T50）

## Resolution

**验证通过**：快照 diff 仅 +1；全仓 mvn verify BUILD
SUCCESS 三门绿；TSession6000LedgerAuditTest 含 6050 全绿；
origin/main 推送封卷。
