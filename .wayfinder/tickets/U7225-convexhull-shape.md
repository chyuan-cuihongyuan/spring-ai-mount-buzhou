---
id: U7225
title: U 会话 U13 ConvexHull 凸包的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

外包络怎么线性对排序？（spec 7012 / effort #7012 / U13）

## Resolution

**ConvexHull（core/policy）**：monotone chain 上下链
双扫 O(n log n)；严格凸包；退化诚实降级；±10^9 域 fail-fast。
