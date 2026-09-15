---
id: T2692
title: 重试抖动实效读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2691
created: 2026-09-15
---

## Question

RetrySpreadStats 怎么验证？（spec 1745 验收/裁决）

## Resolution

RetrySpreadStatsTest：恒定=0/散布=2/退化与负值/全零特判。
