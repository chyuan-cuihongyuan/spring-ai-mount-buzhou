---
id: T1149
title: 取消原因分布读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

五类取消原因的分布面怎么做？平局 dominant 语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 25 轮 = effort #824 / spec 824 / impl 577）：`CancelCauseDistribution`——synchronized 计数+lastSeen(max)+share 降序+dominant（平局=枚举声明序先者）；闭集天然有界；null 忽略；喂点归装配侧。
