---
id: T1185
title: HITL 认证决策分布的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

认证决策统计的五态口径与喂点如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 43 轮 = effort #842 / spec 842 / impl 595）：`AuthDecisionStats`——五态闭集（GRANTED/DENIED/EXPIRED/CONSUMED/UNKNOWN）计数+占比降序快照；null 忽略；喂点=GuardAuthApi 装配侧零变更。
