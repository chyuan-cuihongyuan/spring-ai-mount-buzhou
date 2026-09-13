---
id: T1196
title: 数据集近重复读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1195]
created: 2026-09-13
---

## Question

成簇/唯一率/截断如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 48 轮 = effort #847）：DatasetNearDuplicateStatsTest 6 例——3 选 2 对+unique 0.5/近重复 0.8 一对/全唯一 1.0/条目封顶截断/脏条目/fail-fast。
