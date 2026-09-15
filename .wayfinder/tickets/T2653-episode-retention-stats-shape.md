---
id: T2653
title: 情节保留普查的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

EpisodeRetentionStats 的形状怎么裁决？（spec 1726 / effort #1726 / R27）（spec 1726 验收/裁决）

## Resolution

RetentionEvent 三闭集 STORED/EVICTED_TTL/EVICTED_CAPACITY+record(event,n) 批量记账（n<0 忽略）+census+evictRatio −1 哨兵+resetForTest——Kafka retention 思想，TTL vs 容量逐出归因。
