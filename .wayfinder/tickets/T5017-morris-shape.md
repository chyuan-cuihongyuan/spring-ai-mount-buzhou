---
id: T5017
title: Q 会话 R9 Morris 近似计数的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

海量低价值计数怎么省空间省写放大？（spec 3008 / effort #3008 / R9）

## Resolution

**MorrisCounter（core/metrics）**：概率计数换空间——只存指数 v，
增量 2^−v 概率才 +1（越大越懒），估计 2^v−1 期望无偏；O(log log n)
空间+写放大对数衰减+指数封顶 62 诚实边界+RandomGenerator 注入。
精确计数该用 long——本件换的是空间与写放大。
