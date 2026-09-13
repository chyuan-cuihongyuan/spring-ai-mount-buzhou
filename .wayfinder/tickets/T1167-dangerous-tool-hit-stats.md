---
id: T1167
title: 危险工具命中分布的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

命中热力排行怎么聚合？溢出口径与喂点如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 34 轮 = effort #833 / spec 833 / impl 586）：`DangerousToolHitStats`——工具封顶 64 超限并入 __overflow__（跨域口径一致）；hits/requiredState 最近非空/lastSeen max+top(n) 降序典序破平；喂点=GuardHook 装配侧不改拦截。
