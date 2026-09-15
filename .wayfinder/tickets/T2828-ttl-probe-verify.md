---
id: T2828
title: TTL 探针状态机的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2827]
created: 2026-09-16
---

## Question

三态/边界/新鲜度/普查/畸形五面正确吗？（spec 1813 / effort #1813 / R14）

## Resolution

**TtlProbeStateMachineTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=TtlProbeStateMachineTest）：三态+双边界含上；freshness 线性递减到期
钳 0；census 计数+criticalRatio+null 哨兵；负年龄/TTL<1/warnFraction 越界与
NaN/空 id fail-fast。

