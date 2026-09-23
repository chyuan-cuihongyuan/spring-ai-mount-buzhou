---
id: T3041
title: 复合健康分的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

多维健康读数怎么合成单一分？（spec 1920 / effort #1920 / R121）

## Resolution`

**监控面板复合健康分惯例纯计算 `HealthScoreComposite`
（core/health）**：composite（Σ(score×w)/Σw 加权平均 0–100）+
band 三档判级（≥80 HEALTHY/≥50 DEGRADED/否则 UNHEALTHY 边界含下）。
同长非空/分数越界/零权重 fail-fast。落轮 grep 复核无占坑。
