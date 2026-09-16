---
id: T3205
title: 文本编辑距离的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

文本差异的精确口径怎么统一原语化？（spec 2052 / effort #2052 / R53）

## Resolution

**Levenshtein 纯函数 `TextDistance`（core/metrics）**：levenshtein（插删
改各 1，两行 DP 滚动 O(min) 空间，空串口径）+similarity ∈[0,1]（1−
dist/maxLen 双空=1）+isNearMatch 阈值判定（0.8 默认与 config 纠错
同口径）——ConfigDoctor 内联实现后续收敛方向。
