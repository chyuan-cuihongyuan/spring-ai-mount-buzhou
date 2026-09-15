---
id: T2806
title: 轮墙钟预算传播的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2805]
created: 2026-09-16
---

## Question

传播三规则（截断/耗尽即拒/前缀性）在五类输入下行为正确吗？（spec 1802 / effort #1802 / R3）

## Resolution

**TurnDeadlineBudgetTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=TurnDeadlineBudgetTest）：截断+耗尽拒绝+获准率；前缀性（早花光后续
全拒）；零预算拒绝一切含零耗；空表/null 哨兵；负预算/负预估 fail-fast。

