---
id: U7289
title: U 会话 U45 FloydCycleDetector 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

判圈怎么 O(1) 空间？（spec 7044 / effort #7044 / U45）

## Resolution

**FloydCycleDetector（core/metrics）**：快慢指针相遇+头部二次迭代定入口；无环 −1；随机图 vs HashSet 圣像。
