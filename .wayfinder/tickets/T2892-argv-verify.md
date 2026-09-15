---
id: T2892
title: argv 预算门的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2891]
created: 2026-09-16
---

## Question]

双闸在字节账/三态先序/边界/畸形四面下正确吗？（spec 1845 / effort #1845 / R46）

## Resolution

**ArgvBudgetGateTest 3 用例全绿**（mvn -pl buzhou-tools test
-Dtest=ArgvBudgetGateTest）：字节账 3+4 含 NUL；单参 131073 判
OVER_SINGLE_ARG（先于总量）、101 参 202B>200 判 OVER_TOTAL、== 边界 FIT；
负预算/零上限/null 元素 fail-fast。

