---
id: T2812
title: O 系 R6 对账轮的验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2811]
created: 2026-09-16
---

## Question

全仓 clean verify BUILD SUCCESS 且三门（覆盖/快照/对账）全绿吗？（spec 1805 / effort #1805 / R6）

## Resolution

**mvn clean verify BUILD SUCCESS**（16+1 模块）+
OSession1800LedgerAuditTest 四断言绿（spec 1800–1805 票对/impl/README/
严格递增）+ 快照门绿（snapshot 与四新类型一致）——三门全绿，Wave 2 无欠账
带入。
