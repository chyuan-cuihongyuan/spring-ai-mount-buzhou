---
id: T2803
title: Spill PSI 失速读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

Spill 压力「疼不疼」怎么量化？（spec 1801 / effort #1801 / R2）

## Resolution

**Linux PSI some/full 双档纯读面 `SpillPressureStall`（buzhou-spill）**：
`StallSample(active, stalled)` 单窗事实（构造器核 0≤stalled≤active 契约，
畸形 fail-fast）；`analyze` → PsiReport（windows/someWindows/fullWindows/
worstStalled/worstActive + somePct/fullPct/worstStallRatio，空观测 -1 哨兵）。
some=≥1 会话失速（吞吐损失）、full=活跃全失速（进度损失）；空闲窗只进分母。

