---
id: T5009
title: Q 会话 R5 top-p 核采样的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

生成/路由随机性的截断怎么按累积质量自适应？（spec 3004 / effort #3004 / R5）

## Resolution

**NucleusSampler（core/policy）**：累积质量 ≥p 的最小核截断（尖峰
窄核/平坦宽核自适应——top-k 一刀切盲区的根治）+核内重归一抽取+
p→0 退化 top-1 / p=1 全分布连续插值+keptCount 核大小确定性读数+
−∞ 零质量永不中+RandomGenerator 注入回放。
