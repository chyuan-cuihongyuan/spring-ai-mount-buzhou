---
id: T2616
title: 会话年龄分桶直方的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2615
created: 2026-09-15
---

## Question

SessionAgeHistogram 怎么验证？（spec 1707 验收/裁决）

## Resolution

SessionAgeHistogramTest：默认桶四带互斥各一例/负值忽略/自定义边界 n+1 桶/eldest 哨戒。
