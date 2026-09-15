---
id: T2834
title: 负载脱落阶梯的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2833]
created: 2026-09-16
---

## Question

阶梯裁决在爬升/边界/哨兵/畸形四面下正确吗？（spec 1816 / effort #1816 / R17）

## Resolution

**LoadShedLadderTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=LoadShedLadderTest）：负载 0.3/0.6/1.5 三档逐级脱落；阈值边界含上
（== 即甩）；空表/null 哨兵；负与 NaN 负载因子/空白名/负阈值 fail-fast。

