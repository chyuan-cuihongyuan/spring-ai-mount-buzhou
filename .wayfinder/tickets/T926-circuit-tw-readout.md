---
id: T926
title: 熔断时间窗生效读面的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

circuit.time-window 声明后（spec 620）生效值健康面不可读（0 与 90s 都是「配了不知道」）。补吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 39 轮 = effort #600 / spec 638 / impl 491）：`stats.details().circuitTimeWindowMs`（configure 一次性写入；0=count 窗缺省、正数=声明值）——与 rateLimitBackend 同款生效确认面。
