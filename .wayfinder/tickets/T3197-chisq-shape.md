---
id: T3197
title: 卡方均匀性检验的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

分布均匀性偏离怎么统计可判定？（spec 2048 / effort #2048 / R49）

## Resolution

**Pearson χ² 纯函数检验 `ChiSquareUniformity`（core/eval）**：
test(observed) → χ²=Σ(O−E)²/E + 自由度 k−1 + 0.05 显著水平临界值表
（内置 1–20，桶 2–21）+ statistic>critical 即拒绝均匀——评估输出偏斜/
哈希分桶负载/采样公平性的偏斜判定统一口径。
