---
id: T2614
title: fork 树形态普查的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2613
created: 2026-09-15
---

## Question

ForkShapeStats 怎么验证？（spec 1706 验收/裁决）

## Resolution

ForkShapeStatsTest：空表哨兵 −1/单根链深度/宽扇出多叶/森林多根/环拒绝 五组断言。
