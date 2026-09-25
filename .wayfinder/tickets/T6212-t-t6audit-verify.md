---
id: T6212
title: T 会话 T6 周期对账的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6211]
created: 2026-09-26
---

## Question

T6 合同怎么逐一验绿？（spec 6005 / effort #6005 / T6）

## Resolution

**验证通过**：全仓 16 模块 mvn verify BUILD SUCCESS（三门
全绿——覆盖门/快照门/对账门）+ TSession6000LedgerAuditTest
核账绿（spec 6000–6003 四件套零缺位严格递增）。
