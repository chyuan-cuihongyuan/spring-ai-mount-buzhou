---
id: T2658
title: 压缩触发原因分布的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2657
created: 2026-09-15
---

## Question

CompactionTriggerStats 怎么验证？（spec 1728 验收/裁决）

## Resolution

CompactionTriggerStatsTest：空哨兵/五笔 2:1:1:1+idleShare=0.4/reset。
