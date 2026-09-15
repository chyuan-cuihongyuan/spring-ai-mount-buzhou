---
id: T2842
title: 重启错峰计划的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2841]
created: 2026-09-16
---

## Question

错峰排程在稳定/多样/碰撞/哨兵/畸形五面下正确吗？（spec 1820 / effort #1820 / R21）

## Resolution

**RestartSpreadPlanTest 5 用例全绿**（mvn -pl buzhou-resilience test
-Dtest=RestartSpreadPlanTest）：同 id 同延迟且 [0,window)；8 实例 16 槽
≥4 不同延迟；鸽笼碰撞账（5 实例 2 槽 ≥3）；空批/null 哨兵；空白 id/
cohort<1/窗<1 fail-fast。

