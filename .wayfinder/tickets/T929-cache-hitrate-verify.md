---
id: T929
title: 命中率 getter 验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T928
created: 2026-09-12
---

## Question

命中率语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（CacheHitRateTest 2/2 + resilience 全模块 245/245 零回归）：Response 3 中 1 失=0.75、零请求 0.0；Semantic 命中/未命中各 1=0.5。
