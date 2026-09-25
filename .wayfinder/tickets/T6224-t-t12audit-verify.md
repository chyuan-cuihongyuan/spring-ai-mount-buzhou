---
id: T6224
title: T 会话 T12 周期对账的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6223]
created: 2026-09-26
---

## Question

T12 合同怎么逐一验绿？（spec 6011 / effort #6011 / T12）

## Resolution

**验证通过**：全仓 16 模块 mvn verify BUILD SUCCESS（三门
全绿）+ TSession6000LedgerAuditTest 核账绿（spec 6000–6010
十一轮四件套零缺位严格递增）。
