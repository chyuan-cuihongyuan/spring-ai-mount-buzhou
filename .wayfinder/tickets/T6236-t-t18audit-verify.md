---
id: T6236
title: T 会话 T18 周期对账的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6235]
created: 2026-09-26
---

## Question

T18 合同怎么逐一验绿？（spec 6017 / effort #6018 / T18）

## Resolution

**验证通过**：全仓 16 模块 mvn verify BUILD SUCCESS（三门
全绿）+ TSession6000LedgerAuditTest 核账绿（spec 6000–6016
十八轮四件套零缺位严格递增）。
