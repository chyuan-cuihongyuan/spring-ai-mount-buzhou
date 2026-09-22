---
id: T2972
title: 自保模式门的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2971]
created: 2026-09-23
---

## Question)

自保门在触发/恢复/边界/畸形四面下正确吗？（spec 1885 / effort #1885 / R86）

## Resolution`

**SelfPreservationGateTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=SelfPreservationGateTest）：低比率停逐/达标续逐两态；边界
恰等正常逐；自保态读数与比率读数；畸形三型（实例 0/阈值 0/阈值 1）
fail-fast。
