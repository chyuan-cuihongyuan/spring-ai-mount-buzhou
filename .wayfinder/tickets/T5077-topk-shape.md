---
id: T5077
title: Q 会话 R39 top-k 采样的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

候选宽度与集内锐度怎么正交调？（spec 3038 / effort #3038 / R39）

## Resolution

**TopKSampler（core/policy，纯函数+注入）**：前 k 名固定宽度截断
+温度缩放 softmax——k 管宽度 T 管锐度四象限（k=1 恒 argmax/T→0
贪心/T 大均匀/集外零）；与 NucleusSampler（自适应宽度 p）成对
互补；probabilities 读数可审计。
