---
id: T907
title: 并发上限装配验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T906
created: 2026-09-12
---

## Question

装配语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（McpConcurrencyAssemblyTest 3/3 + mcp 全模块 50/50 零回归）：

- yml 声明 =2 绑定进属性；缺省 null 且既有缺省（shutdownBudget 35s）不回退。
- 0/负值装配 fail-fast。
