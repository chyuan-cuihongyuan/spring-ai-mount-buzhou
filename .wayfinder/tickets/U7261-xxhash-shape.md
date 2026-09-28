---
id: U7261
title: U 会话 U31 XxHash64 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

高质量快哈希怎么落？（spec 7030 / effort #7030 / U31）

## Resolution

**XxHash64（core/message）**：四累加器条带旋转混合+雪崩终洗；官方钉子向量；质数模 2⁶⁴ 补码入 long（勘误）。
