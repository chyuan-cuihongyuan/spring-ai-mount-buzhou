---
id: U7251
title: U 会话 U26 PatienceLis 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

LIS 怎么免 O(n²)？（spec 7025 / effort #7025 / U26）

## Resolution

**PatienceLis（core/metrics）**：二分找堆 O(n log n)；严格递增；topIndex 父链回溯实例 canonical。
