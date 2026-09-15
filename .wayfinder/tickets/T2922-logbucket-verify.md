---
id: T2922
title: 对数分桶直方图的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2921]
created: 2026-09-16
---

## Question]

桶界分位数在桶号/误差界/两端/畸形四面下正确吗？（spec 1860 / effort #1860 / R61）

## Resolution`

**LogBucketHistogramTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=LogBucketHistogramTest）：γ=2 桶号 0/1/1/2+0.5 负桶；1000 均匀
样本 P95 实际相对误差 < 0.25 保守界（对照精确最近秩）；q=0.5/1.0 两端
可用；0/负值/γ≤1/q 越界/空样本 fail-fast。

