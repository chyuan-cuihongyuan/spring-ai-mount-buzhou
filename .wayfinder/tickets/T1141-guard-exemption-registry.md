---
id: T1141
title: 护栏豁免登记面的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

豁免做通用登记面还是各 hook 内置？过期/续期语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 21 轮 = effort #820 / spec 820 / impl 573）：`GuardExemptionRegistry`——机制×主体显式有时限；惰性过期计数；同键覆盖续期；封顶 64+truncated；不自动接线 hook（默认零变化）。换题注记：原校验聚合半撞。
