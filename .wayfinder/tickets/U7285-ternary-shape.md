---
id: U7285
title: U 会话 U43 TernarySearch 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

单峰极值怎么免网格扫描？（spec 7042 / effort #7042 / U43）

## Resolution

**TernarySearch（core/metrics）**：双探点舍 1/3 区间 O(log)；max/min 对称；单峰假设明示；fail-fast。
