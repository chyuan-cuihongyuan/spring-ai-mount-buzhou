---
id: T2688
title: spill 句柄驻留年龄直方的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2687
created: 2026-09-15
---

## Question

SpillHandleAgeHistogram 怎么验证？（spec 1743 验收/裁决）

## Resolution

SpillHandleAgeHistogramTest：四桶各一例/负值忽略。
