---
id: T6284
title: T 会话 T42 周期对账的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6285]
created: 2026-09-26
---

## Question

T42 合同怎么逐一验绿？（spec 6042 / effort #6042 / T42）

## Resolution

**验证通过**：全仓 16 模块 mvn verify BUILD SUCCESS（三门
全绿）+ TSession6000LedgerAuditTest 核账绿（spec 6000–6041
零缺位严格递增）。
