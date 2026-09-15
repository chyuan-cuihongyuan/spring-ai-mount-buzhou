---
id: T2940
title: 流式中位数保持器的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2939]
created: 2026-09-16
---

## Question]

流式中位在奇偶/序无关/抗偏/畸形四面下正确吗？（spec 1869 / effort #1869 / R70）

## Resolution`

**MedianKeeperTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=MedianKeeperTest）：1→5、+1→3（偶均）、+9→5；乱序流 7,2,9…
与 1..9 同中位 5；9 样本+2 个百万级长尾中位仍 6；空 -1/零数与 NaN/Inf
fail-fast。

