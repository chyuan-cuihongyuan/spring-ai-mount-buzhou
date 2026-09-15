---
id: T2678
title: PII 扫描耗时分位读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2677
created: 2026-09-15
---

## Question

PiiScanLatency 怎么验证？（spec 1738 验收/裁决）

## Resolution

PiiScanLatencyTest：5 笔分位账/偶数中位取均值/空与负值哨兵。
