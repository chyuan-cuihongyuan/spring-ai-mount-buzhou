---
id: T6243
title: T 会话 T22 Hilbert Curve 希尔伯特曲线的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

二维排序怎么保持空间局部性？（spec 6021 /
effort #6021 / T22）

## Resolution

**HilbertCurve（core/policy，源码 T18 预载）**：标准 skew
递归双射（无查表无浮点）——相邻格索引差有界；index/
coordinate 互逆；order≤31；阶/坐标/索引越界 fail-fast。
