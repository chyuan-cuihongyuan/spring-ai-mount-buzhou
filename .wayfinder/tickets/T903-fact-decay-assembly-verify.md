---
id: T903
title: 衰减装配验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T902
created: 2026-09-12
---

## Question

装配语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（FactDecayAssemblyTest 2/2 + 移驻后 DecayingFactStoreTest 5/5 + core/guard/memory 全模块零回归）：

- yml 声明 half-life-turns=4：module.factStore() 半衰过滤生效（低置信陈年滤、新鲜留）。
- 缺省：全注入（既有语义零变化）。
