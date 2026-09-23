---
id: T6075
title: R 会话 R38 Holt-Winters 季节指数的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

周期性负载怎么在线学习季节项追上节律？（spec 4037 /
effort #4037 / R38）

## Resolution

**HoltWintersIndex（core/metrics）**：加法 Holt-Winters 三参数
在线更新（水平/趋势/季节）+ statsmodels 式两季预热启发式 +
forecast(h) 与季节指数读数；预热未毕业诚实 ISE 不猜。
