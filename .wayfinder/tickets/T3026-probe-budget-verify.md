---
id: T3026
title: 探测流量预算的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3025]
created: 2026-09-23
---

## Question)

占比与判定在边界/畸形下正确吗？（spec 1912 / effort #1912 / R113）

## Resolution`

**ProbeBudgetTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=ProbeBudgetTest）：占比 0.05 与 0.12；判定两态含上边界恰
maxShare 即 WITHIN；畸形三型 fail-fast。
