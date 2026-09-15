---
id: T2636
title: 虚拟键份额读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2635
created: 2026-09-15
---

## Question

VirtualKeyShareStats 怎么验证？（spec 1717 验收/裁决）

## Resolution

VirtualKeyShareStatsTest：空哨兵/均分 HHI=0.25/独占 HHI>0.8 降序保序/单键 HHI=1/_anonymous_ 桶。
