---
id: T2814
title: 检查点滞后读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2813]
created: 2026-09-16
---

## Question

滞后账目在聚合/并列/健康态/哨兵/畸形五面下正确吗？（spec 1806 / effort #1806 / R7）

## Resolution

**CheckpointLagReadoutTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=CheckpointLagReadoutTest）：聚合+越限+追平率；并列最坏取首；全追平
健康态；空表/null 哨兵；负计数/检查点超前/空 id fail-fast。

