---
id: T1195
title: 数据集近重复读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

近重复对账的算法口径（相似度/成簇/封顶）如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 48 轮 = effort #847 / spec 847 / impl 600）：`DatasetNearDuplicateStats`——trigram Jaccard ≥threshold+并查集成簇；对明细封顶 32/条目封顶 200 截断+truncated；脏条目跳过计数；threshold fail-fast。
