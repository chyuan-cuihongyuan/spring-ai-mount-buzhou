---
id: U7277
title: U 会话 U39 ChiSquareUniformity 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

随机质量怎么量化？（spec 7038 / effort #7038 / U39）

## Resolution

**ChiSquareUniformity（core/metrics）**：χ² 统计量+逐桶贡献；期望 ≥1 下限；不假装 p 值精确；越域 fail-fast。
