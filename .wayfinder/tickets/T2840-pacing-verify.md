---
id: T2840
title: 花费匀速曲线的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2839]
created: 2026-09-16
---

## Question

匀速判态在三态/边界/端点/畸形四面下正确吗？（spec 1819 / effort #1819 / R20）

## Resolution

**BudgetPacingCurveTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=BudgetPacingCurveTest）：三态+偏离（容差断言）+运行率（OVER 时 2.0）；
边界含（0.55−0.5 尾差不翻态）；周期首（runRate -1 哨兵）尾（=1.0）端点；
分数越界/NaN/负容差 fail-fast。首跑双红为浮点病理实证，EPSILON 修正后绿。

