---
id: T1193
title: 死信重投成功率读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

重投结果统计口径与喂点如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 47 轮 = effort #846 / spec 846 / impl 599）：`DeadLetterRedeliveryStats`——attempts/successes/成功率/streak 原子记账；喂点=重投路径装配侧；重投语义归调用方。
