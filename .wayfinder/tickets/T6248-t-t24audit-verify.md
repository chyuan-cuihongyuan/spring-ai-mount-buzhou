---
id: T6248
title: T 会话 T24 周期对账的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6247]
created: 2026-09-26
---

## Question

T24 合同怎么逐一验绿？（spec 6023 / effort #6023 / T24）

## Resolution

**验证通过**：全仓 16 模块 mvn verify BUILD SUCCESS（三门
全绿）+ TSession6000LedgerAuditTest 核账绿（spec 6000–6022
零缺位严格递增）。
