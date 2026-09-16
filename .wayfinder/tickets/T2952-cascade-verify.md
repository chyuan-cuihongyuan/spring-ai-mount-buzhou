---
id: T2952
title: 级联失败暴露读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2951]
created: 2026-09-16
---

## Question]

脆性面在权重排序/活静分诊/哨兵/畸形四面下正确吗？（spec 1875 / effort #1875 / R76）

## Resolution`

**CascadeExposureTest 4 用例全绿**（mvn -pl buzhou-resilience test
-Dtest=CascadeExposureTest）：0.4 vs 0.02 最脆边+全图 0.42（容差断言
——浮点假红一次修）；下游不健康激活 1 边 0.5；空表哨兵+并列取首；
空白节点/比率越界与 NaN fail-fast。

