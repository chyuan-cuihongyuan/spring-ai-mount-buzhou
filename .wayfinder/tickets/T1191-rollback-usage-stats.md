---
id: T1191
title: Prompt 回滚使用读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

回滚使用统计与版本机制如何分工？封顶口径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 46 轮 = effort #845 / spec 845 / impl 598）：`RollbackUsageStats`——名封顶 64+溢出桶；rollbacks/lastFrom/lastTo/lastSeen；次数降序典序破平；喂点=回滚执行处装配侧。
