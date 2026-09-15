---
id: T2664
title: 思考占比读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2663
created: 2026-09-15
---

## Question

ThinkingRatioStats 怎么验证？（spec 1731 验收/裁决）

## Resolution

ThinkingRatioStatsTest：累计 0.4+最近 0.6/非法输入钳制与忽略/空哨兵。
