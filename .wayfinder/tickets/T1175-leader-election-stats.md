---
id: T1175
title: 选举竞争读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

选举竞争统计面怎么做？四态归类与烈度口径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 38 轮 = effort #838 / spec 838 / impl 590）：`LeaderElectionStats`——四态原子计数+contentionRatio 烈度；归类归调用方（不改选举行为）；null 忽略空真。
