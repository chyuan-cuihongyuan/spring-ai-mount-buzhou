---
id: T3029
title: LSM 写放大读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

写放大因子与压实债怎么读？（spec 1914 / effort #1914 / R115）

## Resolution`

**RocksDB/LSM WAF 语义纯计算 `WriteAmplificationFactor`
（core/cleanup）**：waf（落盘/逻辑写入比值）+ compactionDebtRatio
（待压实/盘容量压实债）+ needsThrottle（WAF ≥ 阈值限速建议）。
written≥0/logical≥1/capacity≥1/阈值≥1 fail-fast。落轮 grep 复核
半衰期被 FactDecayPolicy 占坑换静脉。
