---
id: T2187
title: Saga 运行静态读数（CompensatingBatch 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 43 轮：补偿型事务运行漏斗读面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：CompensatingBatch 只有按步 tag counter；运行级漏斗（runs/successes/补偿触发/补偿失败断点）无聚合。

形状裁决：CompensatingBatch.sagaStats() 静态面——漏斗四计数+stepsExecuted+守恒 conserved()（runs=successes+compensationRuns）+lastFailedStep（currentStep 追踪：进步更新/失败定格）+resetSagaStatsForTest；unwind 补偿失败分支补计数埋点（评审修正：初版漏埋该分支致失败桶恒 0）。

Out of scope：per-step 分布；补偿耗时；UnitOfWork 维度。
