---
id: T923
title: 衰减过滤观测验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T922
created: 2026-09-12
---

## Question

计数语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（DecayingFactStoreTest 5/5 含计数断言 + core/guard 全模块零回归）：半衰序列累计 = 每次读取中被滤事实数之和（b 滤于 turn4×2+turn8、a 滤于 turn9 → 5）。
