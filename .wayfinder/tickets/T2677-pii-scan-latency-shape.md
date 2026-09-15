---
id: T2677
title: PII 扫描耗时分位读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

PiiScanLatency 的形状怎么裁决？（spec 1738 / effort #1738 / R39）（spec 1738 验收/裁决）

## Resolution

实例面 record(scanMillis) 负值忽略+report(samples/median/p95 最近秩/max −1 哨兵)——Envoy per-filter 计时，与 PiiHitStats 命中面互补。
