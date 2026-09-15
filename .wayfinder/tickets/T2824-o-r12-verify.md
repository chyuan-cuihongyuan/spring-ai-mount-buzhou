---
id: T2824
title: O 系 R12 对账轮的验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2823]
created: 2026-09-16
---

## Question

全仓 clean verify BUILD SUCCESS 且三门全绿吗？（spec 1811 / effort #1811 / R12）

## Resolution

**mvn clean verify BUILD SUCCESS 一次过绿**（快照补登前置生效——
对比 R6 跑了两遍）+ OSession1800LedgerAuditTest 四断言绿（spec 1800–1811
票对/impl/README/严格递增）+ 快照门绿（五新类型一致）。
