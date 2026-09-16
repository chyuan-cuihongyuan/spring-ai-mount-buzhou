---
id: T3190
title: 预热斜坡的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3189]
created: 2026-09-17
---

## Question

WarmupRamp 合同（起点/线性/期满/单调/畸形）怎么钉住？（spec 2044 / effort #2044 / R45）

## Resolution

**七用例一次全绿**（buzhou-core）：elapsed=0 恰 0.1 / 中点 0.55 与
1/4 点 0.325 线性钉死 / 恰 10s 期满 1.0 且 999s 恒满 + warmedUp
9999 拒 10000 过 / 0–1200ms 逐步采样单调不减 / 回拨 −100 宽进 0.5 /
warmupMillis/startFactor 构造回显 / 畸形三型（warmup 0、factor 0、
factor 1.0）fail-fast。
