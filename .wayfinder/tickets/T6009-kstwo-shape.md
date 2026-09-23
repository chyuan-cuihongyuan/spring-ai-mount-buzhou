---
id: T6009
title: R 会话 R5 KS 两样本的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

不假设形状不分桶的两样本全分布对比怎么做？（spec 4004 / effort #4004 / R5）

## Resolution

**KsTwoSample（core/eval，纯静态）**：Kolmogorov-Smirnov——双样本
ECDF 最大竖直距离 D=sup|F₁−F₂|（排序双指针），p 走渐近
Kolmogorov 分布（NR 小样本校正；λ<0.4 诚实 1）。与 Wilson/
ChiSquare 成统计三尺；t 检验均值盲区与卡方差桶主观两病的免设一尺。
