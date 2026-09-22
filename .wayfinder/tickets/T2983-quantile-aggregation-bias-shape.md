---
id: T2983
title: 分位数聚合偏差审计的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

分位平均的偏差量怎么入账？（spec 1891 / effort #1891 / R92）

## Resolution`

**Prometheus/M3 分位不可加语义纯计算 `QuantileAggregationBias`
（core/metrics）**：naiveAverage（错误口径本身可示众）+ biasRatio
（(naive−actual)/actual 符号化偏差）+ isMateriallyBiased（|比| ≥
容差判定）。空表/负值/actual=0 fail-fast。落轮 grep 复核卡方占坑
换静脉。
