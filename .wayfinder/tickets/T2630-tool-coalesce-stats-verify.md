---
id: T2630
title: 工具合并节省读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2629
created: 2026-09-15
---

## Question

ToolCoalesceStats 怎么验证？（spec 1714 验收/裁决）

## Resolution

ToolCoalesceStatsTest：空哨兵/4+2 组记账+1 不记/时延负值忽略/reset。
