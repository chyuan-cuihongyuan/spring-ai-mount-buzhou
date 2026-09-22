---
id: T2989
title: 尾时延放大读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

并行扇出下分位的幂次放大怎么算？（spec 1894 / effort #1894 / R95）

## Resolution`

**The Tail at Scale 幂次放大纯计算 `TailAmplification`
（core/metrics）**：endToEndProbability（q^N 全盒概率）+
requiredPerBoxQuantile（target^(1/N) SLO 反解单盒分位）。独立性
假设诚实入档；q/target∈(0,1]/N≥1 fail-fast。落轮 grep 复核无占坑。
