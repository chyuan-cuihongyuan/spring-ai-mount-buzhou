---
id: T2880
title: 反熵分歧账的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2879]
created: 2026-09-16
---

## Question]

分歧账在四桶/健康/空比对三面下正确吗？（spec 1839 / effort #1839 / R40）

## Resolution

**AntiEntropyDivergenceTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=AntiEntropyDivergenceTest）：2/1/1/1 四桶+工作量 4+一致率 0.2；
全一致工作量 0 一致率 1；空比对/null 哨兵+单侧空全量分歧。

