---
id: T6200
title: S 会话 S50 收口对账的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6199]
created: 2026-09-25
---

## Question

S50 收口怎么逐一验绿？（spec 5049 / effort #5049 / S50）

## Resolution

**验证通过**：全仓 16 模块 mvn verify BUILD SUCCESS（R48
协议口径）；SSession5000LedgerAuditTest 台账核账绿（spec
5000–5049 全五十轮零缺位、严格递增终验）；快照门 +1 后
全绿；50/50=100%——S 会话全闭合。
