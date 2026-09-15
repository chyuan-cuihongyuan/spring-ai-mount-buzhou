---
id: T2858
title: 平滑加权轮询序列的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2857]
created: 2026-09-16
---

## Question]

派发序在比例/平滑/排除/确定性/畸形五面下正确吗？（spec 1828 / effort #1828 / R29）

## Resolution

**SmoothWeightedSequenceTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=SmoothWeightedSequenceTest）：5:1:2×80 恰好 50/10/20；60 项无三连
+前 6 含低频；零权重全派 1 且同参同序；零取数空；畸形四型 fail-fast。
首跑编译红（lambda 捕获循环变量）修正后绿。

