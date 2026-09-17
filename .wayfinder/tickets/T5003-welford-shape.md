---
id: T5003
title: Q 会话 R2 Welford 在线方差的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

流式方差怎么单遍稳定累计并可分片合并？（spec 3001 / effort #3001 / R2）

## Resolution

**WelfordAccumulator（core/metrics）**：add 单遍递推（mean/m2，
delta 双用）+merge Chan pairwise 合并（分片↔全量数学等价，空侧
双向恒等、交换律）+双分母口径（样本 n−1/总体 n）+空态/单点 NaN
诚实边界。大偏移+小波动朴素 Σx² 抵消病的根治。
