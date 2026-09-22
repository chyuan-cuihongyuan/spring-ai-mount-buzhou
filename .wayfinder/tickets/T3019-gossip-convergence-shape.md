---
id: T3019
title: 闲谈收敛估算的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

成员变更传播轮数与 fanout 配比怎么算？（spec 1909 / effort #1909 / R110）

## Resolution`

**SWIM gossip 指数传播纯计算 `GossipConvergence`
（core/concurrent）**：roundsToConverge（⌈log_{f+1} N⌉）+
informedAfter（min(N,(1+f)^r) 知情数估计封顶）+ fanoutFor
（⌈N^{1/R}−1⌉ 反解）。nodes/fanout/rounds 正值 fail-fast。指数
近似诚实入档（真实有重叠冗余轮数 ≥ 估计）。
