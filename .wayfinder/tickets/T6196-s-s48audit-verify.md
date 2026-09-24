---
id: T6196
title: S 会话 S48 周期对账的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6195]
created: 2026-09-25
---

## Question

S48 对账怎么逐一验绿？（spec 5047 / effort #5047 / S48）

## Resolution

**验证通过**：全仓 16 模块 mvn verify BUILD SUCCESS（R48
协议口径）；SSession5000LedgerAuditTest 台账核账绿（spec
5000–5046 卌六轮零缺位）；快照门 +5 后全绿；48/50=96%。
