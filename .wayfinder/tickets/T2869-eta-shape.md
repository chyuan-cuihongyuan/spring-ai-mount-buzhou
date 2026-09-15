---
id: T2869
title: ETA 投影的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

长任务「还要多久」怎么投影？（spec 1834 / effort #1834 / R35）

## Resolution

**CI 进度条/带宽估计思想纯投影 `EtaProjection`（core/eval）**：
estimate(done,total,elapsed) → Projection（progress/etaMillis/
projectedTotalMillis），线性外推+除不尽向上取整（保守）；done=0 或
elapsed=0 无速率基准 → -1 哨兵（不编速率）；done=total → ETA 0。
纯投影零采样。

