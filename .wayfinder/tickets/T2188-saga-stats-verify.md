---
id: T2188
title: saga 漏斗守恒与断点步名的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2187
created: 2026-09-14
---

## Question

如何证明漏斗守恒、断点步名与补偿失败分桶？

## Resolution

**用户常设授权 AFK（可推翻）**

`CompensatingBatchSagaStatsTest` 四测全绿（`mvn -pl buzhou-core -am test`）：成功 run（steps=2/successes=1/conserved）；三步第三步爆（compensationRuns=1+断点步名 step-c）；**补偿自身失败单列**（评审修正：初版漏埋 unwind 失败分支计数恒 0——补 COMPENSATION_FAILURES.incrementAndGet）；reset 归零。静态面前后归零防串扰。
