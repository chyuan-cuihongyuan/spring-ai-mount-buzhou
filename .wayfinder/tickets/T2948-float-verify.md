---
id: T2948
title: 调度松弛量的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2947]
created: 2026-09-16
---

## Question]

松弛量在关键零浮动/旁支缓冲/串行/畸形四面下正确吗？（spec 1873 / effort #1873 / R74）

## Resolution`

**ScheduleFloatTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=ScheduleFloatTest）：a/b 关键零浮动、c/d 各 85 缓冲（首跑红为
心算期望误 80——R54 病理第五次实证，按实现口径重算）；串行链全关键；
空/环/端点缺失/重复 fail-fast。

