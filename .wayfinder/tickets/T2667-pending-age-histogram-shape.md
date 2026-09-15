---
id: T2667
title: 待决事件年龄直方的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

PendingAgeHistogram 的形状怎么裁决？（spec 1733 / effort #1733 / R34）（spec 1733 验收/裁决）

## Resolution

实例面桶式房规：默认 1s/10s/1m/5m→5 桶+record 负值忽略+oldestMillis 哨戒——Kafka lag exporter 思想，与 PendingSnapshot 瞬时清单互补。
