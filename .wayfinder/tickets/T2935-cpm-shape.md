---
id: T2935
title: 关键路径长度的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

并行编排「该优化哪个任务」怎么算？（spec 1867 / effort #1867 / R68）

## Resolution`

**项目管理 CPM（关键路径法）思想纯计算 `CriticalPathLength`
（core/exec）**：Task/Dependency 契约 + longestPath（Kahn 拓扑消元 +
EF[v]=dur[v]+max(EF[pred]) DP）→ Result(criticalPathMillis,
terminalTask)；环/端点缺失/重复任务 fail-fast。最长加权路径=总时长
下界：缩非关键白费力、关键延一秒总长延一秒。O(V·E) 前驱查取小图
诚实边界。

