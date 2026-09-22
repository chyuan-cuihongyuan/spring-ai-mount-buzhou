---
id: T2985
title: 阶梯加压计划的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

压测负载曲线的台阶插值怎么确定？（spec 1892 / effort #1892 / R93）

## Resolution`

**k6/Gatling ramping stages 纯计算 `RampProfile`（core/policy）**：
targetAt（台阶内线性爬坡插值、末台阶后保持）+ totalDuration +
peakTarget。台阶时长 ≥ 1/目标 ≥ 0/非空 fail-fast。落轮 grep 复核
慢启动族占坑换静脉。
