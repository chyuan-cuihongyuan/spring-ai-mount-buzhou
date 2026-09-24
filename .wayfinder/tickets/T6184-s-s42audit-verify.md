---
id: T6184
title: S 会话 S42 周期对账的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6183]
created: 2026-09-25
---

## Question

S42 对账怎么逐一验绿？（spec 5041 / effort #5041 / S42）

## Resolution

**验证通过**：全仓 16 模块 mvn verify BUILD SUCCESS（R48
协议口径）；SSession5000LedgerAuditTest 台账核账绿（spec
5000–5040 卅轮零缺位）；快照门 +5 后全绿；42/50=84%。
