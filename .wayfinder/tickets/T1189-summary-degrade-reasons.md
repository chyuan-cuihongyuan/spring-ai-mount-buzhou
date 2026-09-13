---
id: T1189
title: 摘要降级原因分布的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

降级原因五态口径与喂点如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 45 轮 = effort #844 / spec 844 / impl 597）：`SummaryDegradeReasons`——五态闭集（OVER_LIMIT/GENERATION_FAILED/EMPTY_CONTENT/POLICY_FORCED/UNKNOWN）计数+占比降序；null 忽略；喂点=降级管线装配侧零变更。
