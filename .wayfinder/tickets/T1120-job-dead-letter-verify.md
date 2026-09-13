---
id: T1120
title: 作业死信台账验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1119]
created: 2026-09-13
---

## Question

异步回调入账/环形挤老/聚合封顶/观察者隔离如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 10 轮 = effort #809）：JobDeadLetterLogTest 5 例（单线程调度 marker 顺序保证异步断言确定性）+DelayedJobQueueTest 3 例回归绿（向后兼容）。
