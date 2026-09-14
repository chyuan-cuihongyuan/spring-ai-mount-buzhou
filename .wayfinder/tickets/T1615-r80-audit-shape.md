---
id: T1615
title: R80 周期预检轮（J 系 R71–R79 对账 + 全仓 verify）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1611
created: 2026-09-15
---

## Question

J 会话第 80 轮（周期预检第六轮）：审计范围与处置口径？

## Resolution

**用户常设授权 AFK（可推翻）**

形状裁决（R50/R60/R70 对账轮同型）：J 系 R71–R79 工件全量对账 + 隔离 worktree 全仓 `mvn verify` + 双文档门复跑 + 发现就近处置。
