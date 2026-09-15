---
id: T2659
title: todo 返工周期读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

TodoCycleStats 的形状怎么裁决？（spec 1729 / effort #1729 / R30）（spec 1729 验收/裁决）

## Resolution

recordReopen(itemId) 逐项计数有界 128 超出并 _overflow_（null/空归 _anonymous_）+census(touchedItems/totalReopens/worstReopens)——Jira reopened 思想，返工频率与最惨项。
