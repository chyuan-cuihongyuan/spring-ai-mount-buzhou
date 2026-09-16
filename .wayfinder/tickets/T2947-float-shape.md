---
id: T2947
title: 调度松弛量的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

非关键任务能延多久不拖总长怎么算？（spec 1873 / effort #1873 / R74）

## Resolution`

**CPM 松弛量（float/slack）纯计算 `ScheduleFloat`（core/exec，与
CriticalPathLength 配对）**：floats → 逐任务 TaskFloat（floatMillis+
earliestStartMillis）；正向 ES/EF + 反向到汇最长距双向 DP（LS=T−toSink−
dur，float=LS−ES 关键 0）；环（visiting 集）/端点缺失/重复 fail-fast。
float 分布=编排刚性度读数，削峰让路有依据。

