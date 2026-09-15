---
id: T2904
title: 桶表容量阶梯的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2903]
created: 2026-09-16
---

## Question]

三件套在建议幂/边界含/装填/畸形四面下正确吗？（spec 1851 / effort #1851 / R52）

## Resolution

**BucketTableSizingTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=BucketTableSizingTest）：100/0.75→256、3/0.75→4（首跑红为测试
期望算术误 8，修正）、0→1、lf=1→128；3/4 RESIZE vs 2/4 OK 边界含；
load 6/8=0.75；五型畸形 fail-fast。

