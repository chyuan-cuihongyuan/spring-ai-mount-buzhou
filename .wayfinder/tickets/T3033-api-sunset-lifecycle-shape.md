---
id: T3033
title: API 弃用日落生命周期的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

弃用三段生命周期的时刻判定怎么算？（spec 1916 / effort #1916 / R117）

## Resolution`

**Stripe/GitHub 版本日落语义纯计算 `ApiSunsetLifecycle`
（core/policy）**：phase 三段（ACTIVE/DEPRECATED/SUNSET，边界含上）
+ daysRemaining 剩余读数钳 0。deprecatedAt ≤ sunsetAt fail-fast。
与 ToolDeprecation 配置面互补；落轮 grep 复核声明面已存在、判定
面无占坑。
