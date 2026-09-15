---
id: T2686
title: 范围读局部性分类读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2685
created: 2026-09-15
---

## Question

RangeLocalityStats 怎么验证？（spec 1742 验收/裁决）

## Resolution

RangeLocalityStatsTest：4 笔 1 顺 2 随+占比 1/3/首读哨兵/负值忽略。
