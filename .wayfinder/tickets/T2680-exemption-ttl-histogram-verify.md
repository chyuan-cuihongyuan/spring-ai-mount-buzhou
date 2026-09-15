---
id: T2680
title: 豁免 TTL 直方的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2679
created: 2026-09-15
---

## Question

ExemptionTtlHistogram 怎么验证？（spec 1739 验收/裁决）

## Resolution

ExemptionTtlHistogramTest：四桶各一例+永久一笔+负值忽略/自定义边界。
