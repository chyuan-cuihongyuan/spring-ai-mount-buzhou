---
id: T3047
title: 事件循环滞后探针的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

调度器饱和的滞后探针怎么算？（spec 1923 / effort #1923 / R124）

## Resolution`

**Node.js 事件循环滞后探针纯计算 `EventLoopLagProbe`
（core/concurrent）**：lagMillis（executed−scheduled 负滞后钳 0——
定时器合并噪声）+ verdict 两态（≤ threshold OK 边界含上/SATURATED）。
时刻/阈值非负 fail-fast。落轮 grep 复核无占坑。
