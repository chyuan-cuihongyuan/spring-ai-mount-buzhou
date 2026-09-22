---
id: T2986
title: 阶梯加压计划的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2985]
created: 2026-09-23
---

## Question)

爬坡插值在正爬/反爬/稳态/超时/畸形下正确吗？（spec 1892 / effort #1892 / R93）

## Resolution`

**RampProfileTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=RampProfileTest）：[(0,10s),(50,10s),(20,10s)] 五采样点
5s→25/15s→35/25s→20/35s→20 保持；峰值 50 总时长 30s；畸形三型
fail-fast。
