---
id: T2903
title: 桶表容量阶梯的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

自建桶表的初容量/扩容时机怎么定？（spec 1851 / effort #1851 / R52）

## Resolution`

**HashMap 负载因子+2 的幂容量惯例三件套 `BucketTableSizing`
（core/cache）**：suggestCapacity(⌈n/lf⌉ 向上取 2 的幂，0 条目→1)+
verdict(capacity, size, lf)（装填度≥lf 即 RESIZE_NEEDED 边界含——
碰撞链超线性拐点前动手）+ load 装填读数（扩容前瞻）；默认 lf 0.75
常量。纯建议不扩容。

