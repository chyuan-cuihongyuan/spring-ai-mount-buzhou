---
id: T2649
title: 事实读热分桶的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

FactReadHistogram 的形状怎么裁决？（spec 1724 / effort #1724 / R25）（spec 1724 验收/裁决）

## Resolution

实例面逐键计数有界 512 超出并 _overflow_+census 冷(1)/温(2-4)/热(5-16)/灼(>16) 四档+totalReads+distinctKeys，null/空归 _anonymous_——Redis LFU 四档思想，纯读面。
