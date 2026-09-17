---
id: T5002
title: Q 会话 R1 对账门的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5001]
created: 2026-09-18
---

## Question

R1 对账门四向断言怎么逐一验绿？（spec 3000 / effort #3000 / R1）

## Resolution

**验证通过**：QSession3000LedgerAuditTest 四测全绿（票对/impl/
README 覆盖/严格递增——spec 3000 自身即首例通过件）；测试纯
Files 驱动零生产代码改动，快照面不动（1056 基线不变）；R6k
对账轮起入全仓 verify 三门核账。
