---
id: T2819
title: 扇出 pacing 计划的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

并行扇出的惊群避让与小流零延迟怎么两全？（spec 1809 / effort #1809 / R10）

## Resolution

**TCP IW + pacing 思想纯排程 `FanoutPacingPlan`（core/exec）**：`plan(fanout,
interval, headStart)` 前头部名额立即发（IW——小扇出零节流），尾部按
(i−headStart+1)×interval 匀速放行（大扇出摊平到达曲线）；Plan 带
totalSpanMillis 跨度 + pacedRatio 被节流占比（零扇出 -1 哨兵）。两极连续
可调（headStart=fanout 全立即=现状，0 全量 pacing）。纯排程不执行。

