---
id: T2837
title: 顺序读预读顾问的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

预读窗怎么随访问形状自适应？（spec 1818 / effort #1818 / R19）

## Resolution

**Linux readahead 思想纯建议 `ReadAheadAdvisor`（buzhou-spill）**：尾链检测
（相邻读首尾相接成链）判三态 SEQUENTIAL/RANDOM/COLD；SEQUENTIAL 预读 =
blockSize×2^min(链长−1, 3) 指数放大封顶 8 倍；RANDOM 零预读；COLD 样本
不足。常量 MAX_GROWTH_FACTOR_EXPONENT/MIN_CHAIN_FOR_PATTERN 显式；纯建议
零执行。

