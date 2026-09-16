---
id: T2951
title: 级联失败暴露读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

依赖拓扑上「哪里最脆」怎么事前排序？（spec 1875 / effort #1875 / R76）

## Resolution`

**级联失败分析惯例（Hystrix 舱壁思想源头）纯读面 `CascadeExposure`
（buzhou-resilience）**：Edge 契约（流量占比×下游失败率=riskWeight 权重
——该边传导的期望损失面）+ analyze → Exposure（worstEdge 最脆边/
totalWeight 全图损失面/activeRiskyEdges 活风险（下游不健康且权重>0）
vs 静风险（埋着），activeRatio -1 哨兵）。熔断逐点防护之外的拓扑脆性
事前排序。

