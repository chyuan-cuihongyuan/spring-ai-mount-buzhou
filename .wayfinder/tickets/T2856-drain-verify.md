---
id: T2856
title: 排空预测的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2855]
created: 2026-09-16
---

## Question]

makespan 预测在两主导/并列/空/畸形四面下正确吗？（spec 1827 / effort #1827 / R28）

## Resolution

**DrainForecastTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=DrainForecastTest）：单会话主导（1000 拖死 makespan 10s）；并行主导
（41÷2 ceil 21>11 瓶颈 d）；相等取单会话；空/null 零预测；并行度<1/
负耗时/空白 id/负剩余 fail-fast。

