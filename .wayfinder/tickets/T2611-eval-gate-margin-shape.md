---
id: T2611
title: 门限边际直方的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

门判定边际读面的形状怎么裁决？（spec 1705 / effort #1705 / R6）

## Resolution

**静态纯函数 `EvalGateMargin`（core/eval）**：`analyze(passRates, threshold)` →
`MarginReport(runs/threshold/margins/minMargin/maxMargin)` + `withinBand(band)`
危险带计数；两侧对称（|rate−threshold|）；空表哨兵 min/max=−1。借鉴 Google
SRE 告警边际 / SPRT 边际。纯读面不改 EvalGate 判定，带宽宿主声明。
