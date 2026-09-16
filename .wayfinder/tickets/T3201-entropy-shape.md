---
id: T3201
title: 香农熵读数的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

分布多样性怎么连续量纲统一可比？（spec 2050 / effort #2050 / R51）

## Resolution

**Shannon 熵纯函数 `ShannonEntropy`（core/metrics）**：entropy（−Σp·log_b
p 零频不计，base>1 可选 bits/nats）+entropyBits 便捷+normalizedEntropy
∈[0,1]（÷log₂非零类数——全集中 0/均匀 1 跨分布可比）——答案多样性/
路由集中度的连续度量，与卡方互补（量纲 vs 判定）。
