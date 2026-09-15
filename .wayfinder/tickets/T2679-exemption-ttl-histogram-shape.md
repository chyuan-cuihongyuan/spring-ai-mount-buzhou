---
id: T2679
title: 豁免 TTL 直方的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

ExemptionTtlHistogram 的形状怎么裁决？（spec 1739 / effort #1739 / R40）（spec 1739 验收/裁决）

## Resolution

实例面桶式：默认 1m/10m/1h/24h 五桶+permanent 独立计数（负值忽略，0=永久入 permanent 不入桶）——cert-manager 生命周期普查，长期豁免=权限漂移温床。
