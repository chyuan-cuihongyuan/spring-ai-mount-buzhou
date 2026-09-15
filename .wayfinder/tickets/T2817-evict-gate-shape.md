---
id: T2817
title: 驱逐信号阈值门的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

自动驱逐的两级阈值语义怎么安放？（spec 1808 / effort #1808 / R9）

## Resolution

**K8s eviction manager 思想纯裁决 `EvictionThresholdGate`（buzhou-spill）**：
Thresholds(soft, hard) 构造器核 0≤soft≤hard 非 NaN；decide → 三态 BELOW/
GRACE_PENDING/EVICT_NOW（signal≥hard 立即逐不受宽限豁免；signal≥soft 看
millisAboveSoft≥grace）；census 多信号批量三态普查（DecisionCensus+
evictRatio -1 哨兵）。纯裁决不执行，信号口径宿主自声明。

