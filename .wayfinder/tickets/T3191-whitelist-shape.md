---
id: T3191
title: 属性白名单过滤器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

观测属性导出的无限生长怎么主动瘦身？（spec 2045 / effort #2045 / R46）

## Resolution

**OTel View 思想纯函数过滤器 `AttributeWhitelist`（buzhou-observability
pipeline）**：白名单模式盘内保留盘外**分属性计数丢弃**（dropped
显形不静默——治理有对账面）+通配 allowAll（未配置不断流）+空表显式
全拒（收紧口径可表达）+值原样传递+名单非 null 与空表语义区分
fail-fast。
