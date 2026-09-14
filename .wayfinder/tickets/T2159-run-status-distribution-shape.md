---
id: T2159
title: 运行状态分布与滞后审计（RunStatusDistribution）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 30 轮（里程碑轮）：恢复巡检的状态分布与暴露窗口审计面选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：RunRegistry 只有 list(status) 原子查询；状态分布与 turn 滞后（崩溃暴露窗口）无读面。

形状裁决：RunStatusDistribution 纯函数（core/recovery）——analyze→Report(statusHistogram 全枚举预置 0+runningLagMax 仅 RUNNING 维+TurnLag(worst 3 零滞后不入、降序典序))；滞后=currentTurn−lastCompletedTurn（钳 0）；纯函数不触存储。

Out of scope：新鲜度；恢复联动；多 registry 聚合。
