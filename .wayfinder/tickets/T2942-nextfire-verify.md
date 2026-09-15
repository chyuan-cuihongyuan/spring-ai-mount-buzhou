---
id: T2942
title: 固定间隔下次触发的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2941]
created: 2026-09-16
---

## Question]

网格触发在对齐/含上/补账/畸形四面下正确吗？（spec 1870 / effort #1870 / R71）

## Resolution`

**NextFireScheduleTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=NextFireScheduleTest）：250→300/200→200 含上/201→300/epoch
前后即 epoch；补账 (100,450]=3、无错过 0、首格起 3、未到 epoch 0；
间隔<1/负时点/确认越界 fail-fast。

