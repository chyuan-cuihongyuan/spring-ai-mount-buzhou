---
id: T2938
title: 区间调度的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2937]
created: 2026-09-16
---

## Question]

区间面在合并四形态/缝隙三段/覆盖裁剪/畸形四面下正确吗？（spec 1868 / effort #1868 / R69）

## Resolution`

**IntervalScheduleTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=IntervalScheduleTest）：乱序四段合并成 (0,20)+(30,40)；窗口
0-50 三缝 (0,10)/(20,30)/(40,50)；全覆盖零缝+空占用全窗一缝+越界
裁剪 (15,30)；倒置区间/倒置窗口/null 区间 fail-fast。

