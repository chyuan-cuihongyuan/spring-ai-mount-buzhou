---
id: T2846
title: 混沌预算门的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2845]
created: 2026-09-16
---

## Question]

预算门在优先序/边界/账目/畸形四面下正确吗？（spec 1822 / effort #1822 / R23）

## Resolution

**ChaosBudgetGateTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=ChaosBudgetGateTest）：窗口优先（窗外禁、窗内零预算耗尽）；边界含
两端+空窗口；使用账累计/钳零/超支 1.2；负预算/倒挂窗口/null 与负花费
fail-fast。

