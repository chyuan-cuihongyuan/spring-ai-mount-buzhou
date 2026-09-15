---
id: T2855
title: 排空预测的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

优雅停机超时怎么不拍常数？（spec 1827 / effort #1827 / R28）

## Resolution

**k8s drain/Envoy shutdown drain 思想纯预测 `DrainForecast`
（core/session）**：forecast(parallelism, millisPerUnit, work) → makespan
= max(最大单会话剩余, ceil(总剩余÷并行度))×单位耗时 + bottleneckSession
瓶颈直读 + parallelismBound 主导方（严格大于判并行主导，相等取单会话
——并列取更可操作处方）。纯预测零执行。

