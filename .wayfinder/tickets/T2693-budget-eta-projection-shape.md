---
id: T2693
title: 预算耗尽 ETA 投影的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

BudgetEtaProjection 的形状怎么裁决？（spec 1746 / effort #1746 / R47）（spec 1746 验收/裁决）

## Resolution

静态纯函数 project(remaining, recentSpend, intervalMillis)→EtaProjection(avgSpend/etaMillis/verdict 三闭集 NO_DATA/STABLE(avg<=0 eta=−1)/PROJECTED)——Prometheus predict_linear 思想，烧穿前告警。
