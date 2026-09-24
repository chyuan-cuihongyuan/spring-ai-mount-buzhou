---
id: T6172
title: S 会话 S36 周期对账的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6171]
created: 2026-09-24
---

## Question

S36 对账怎么逐一验绿？（spec 5035 / effort #5035 / S36）

## Resolution

**验证通过**：全仓 16 模块 mvn verify BUILD SUCCESS（R48
协议口径）；SSession5000LedgerAuditTest 台账核账绿（spec
5000–5034 卅四轮零缺位）；快照门 +5 后全绿；36/50=72%。
