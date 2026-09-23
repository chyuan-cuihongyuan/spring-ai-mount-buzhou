---
id: T3048
title: 事件循环滞后探针的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3047]
created: 2026-09-23
---

## Question)

滞后判定在采样/边界/畸形下正确吗？（spec 1923 / effort #1923 / R124）

## Resolution`

**EventLoopLagProbeTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=EventLoopLagProbeTest）：滞后 45/0 两例；判定 ≤10 OK/>
10 SATURATED 恰 10 含上；畸形两型 fail-fast。
