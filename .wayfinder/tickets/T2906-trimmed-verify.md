---
id: T2906
title: 截尾均值的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2905]
created: 2026-09-16
---

## Question]

截尾中枢在离群/退化/哨兵/畸形四面下正确吗？（spec 1852 / effort #1852 / R53）

## Resolution

**TrimmedMeanTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=TrimmedMeanTest）：30s 卡顿截尾后中枢 100.5（不被拉到 5s）；零截
退化算术均+n=4 f=0.25 对称截 (2+3)/2；n=1 f=0.4 不截（f<0.5 截不空
数学性质）+空表/null 哨兵；f≥0.5/负 f/null/NaN 样本 fail-fast。首跑红
为哨兵期望误（截空不可达），修正后绿。

