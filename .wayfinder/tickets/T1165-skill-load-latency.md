---
id: T1165
title: 技能加载延迟读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

技能延迟读数与既有计数面如何分工？溢出口径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 33 轮 = effort #832 / spec 832 / impl 585）：`SkillLoadLatency`——per-skill 环 32+P50/P95+max 峰值；超 1024 技能并入 __overflow__ 桶（SkillUsageStats 同款）；loads=近窗语义（累计归计数面）；slowest P95 降序。
