---
id: T3199
title: Gumbel-max 采样器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

按 logits 概率采样怎么免归一化可回放？（spec 2049 / effort #2049 / R50）

## Resolution

**Gumbel-max trick `GumbelMaxSampler`（core/policy，RandomGenerator
注入）**：argmax(logitsᵢ + Gumbelᵢ)（−ln(−ln(u)) 拒绝边界）——数学
等价 softmax 采样免归一化无溢出+-∞ logit 永不中（禁选免掩码）+
empiricalFrequencies 蒙特卡洛对账读数+同种全序列回放。
