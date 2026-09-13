---
id: T1169
title: 上下文截断统计的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

跨机制截断聚合面怎么做？溢出桶语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 35 轮 = effort #834 / spec 834 / impl 587）：`ContextTruncationStats`——开集策略键封顶 8 超限并入溢出桶（量净计）；events/chars 双累计+chars 降序；脏入参忽略；喂点=机制装配侧。
