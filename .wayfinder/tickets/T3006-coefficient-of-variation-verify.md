---
id: T3006
title: 波动系数读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3005]
created: 2026-09-23
---

## Question)

CV 读数在分档/负均值/畸形下正确吗？（spec 1902 / effort #1902 / R103）

## Resolution`

**CoefficientOfVariationTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=CoefficientOfVariationTest）：三档各一例（0.1/0.3/0.5 边界
严格小于）；负均值取绝对值；mean=0 与负 stddev fail-fast。
