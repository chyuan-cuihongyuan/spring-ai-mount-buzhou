---
id: T2294
title: MemoryModule yml 样板统一的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2293
created: 2026-09-15
---

## Question

M 会话第 24 轮：去重如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-memory test` 绿（187 用例——含开关默认值/区间回落/字符串透传全部路径断言零回归）。
