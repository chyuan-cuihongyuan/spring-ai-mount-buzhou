---
id: T901
title: 装配摘要验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T900
created: 2026-09-12
---

## Question

摘要语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（BuzhouAssemblyReportTest 3/3 + core 全模块零回归）：

- 缺省面板：机制开、otel/dashboard 关、store=memory、model=unknown、键数=机制数+2。
- yml 覆盖（关 resilience/store=redis/model-name）进入面板。
- opt-in 门：无属性无 bean；enabled=true 有 bean。
