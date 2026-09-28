---
id: T6296
title: T 会话 T48 周期对账的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6297]
created: 2026-09-28
---

## Question

T48 合同怎么逐一验绿？（spec 6048 / effort #6048 / T48）

## Resolution

**验证通过**：快照 diff 仅 +4 逐行核对；全仓 mvn verify
BUILD SUCCESS 三门绿；TSession6000LedgerAuditTest 台账核账
全绿（spec 6000–6046 零缺位）。
