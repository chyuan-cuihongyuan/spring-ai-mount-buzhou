---
id: T3013
title: 分片偏斜审计的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

分片负载偏斜的量化与重分片触发怎么算？（spec 1906 / effort #1906 / R107）

## Resolution`

**Spark data skew 语义纯计算 `ShardSkewAudit`（core/policy）**：
skewRatio（max/avg 偏斜比，1.0 绝对均衡）+ needsReshard（比率 ≥
阈值触发建议）。loads 非空非负且 avg>0 fail-fast（全零无流量不谈
偏斜）；阈值 ≥1。落轮 grep 复核无占坑。
