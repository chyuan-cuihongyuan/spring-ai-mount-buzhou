---
id: T3107
title: 记忆强度三分量评分的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

记忆检索排序与衰减淘汰的统一强度口径怎么定？（spec 2003 / effort #2003 / R4）

## Resolution

**mem0 三分量纯函数评分 `MemoryStrengthScore`（buzhou-memory recall）**：
recency=2^(−Δt/halfLife) 半衰期衰减（默认 24h）+ frequency=1−1/(1+ln(1+count))
对数饱和 + importance [0,1] 钳制直通；Weights record（非负非全零
fail-fast，默认 0.5/0.3/0.2）加权和归一恒 [0,1]；Δt/count/halfLife
畸形 fail-fast。纯函数零状态——排序键与淘汰键同源一致。
