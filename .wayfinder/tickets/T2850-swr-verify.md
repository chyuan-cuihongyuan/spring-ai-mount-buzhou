---
id: T2850
title: SWR 策略的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2849]
created: 2026-09-16
---

## Question]

SWR 三态在判态/读数/退化/畸形四面下正确吗？（spec 1824 / effort #1824 / R25）

## Resolution

**StaleWhileRevalidatePolicyTest 4 用例全绿**（mvn -pl buzhou-resilience
test -Dtest=StaleWhileRevalidatePolicyTest）：三态+双边界（1000 进陈旧、
1500 过期）；staleness 三段+哨兵（新鲜钳 0）；零窗退化纯 TTL；负值
fail-fast。

