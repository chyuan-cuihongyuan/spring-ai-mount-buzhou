---
id: T6245
title: T 会话 T23 Interpolation Search 插值查找的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

均匀有序数组怎么分布感知探测？（spec 6022 /
effort #6022 / T23）

## Resolution

**InterpolationSearch（core/concurrent，源码 T18 预载）**：
值域线性内插估位（double 先行防溢出+钳制保证正确性）、
等值窗口守卫、重复值任一位次、缺席 -1；null fail-fast。
