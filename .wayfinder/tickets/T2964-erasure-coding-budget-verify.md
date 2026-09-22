---
id: T2964
title: 纠删码冗余预算的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2963]
created: 2026-09-23
---

## Question)

冗余四读数在经典配置/副本对照/畸形下正确吗？（spec 1881 / effort #1881 / R82）

## Resolution`

**ErasureCodingBudgetTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=ErasureCodingBudgetTest）：EC(4,2) 四读数（2/3、2、4、6）；
EC(8,4) 可用率与修复读；EC(1,2) 副本对照（1/3 可用率同 2 容忍）；
畸形三型（data=0/parity=0/负值）fail-fast。
