---
id: T2687
title: spill 句柄驻留年龄直方的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

SpillHandleAgeHistogram 的形状怎么裁决？（spec 1743 / effort #1743 / R44）（spec 1743 验收/裁决）

## Resolution

实例面桶式：默认 1m/1h/1d 四桶+eldestMillis 哨戒+负值忽略——Redis OBJECT IDLETIME 思想，onload 回收跟不上 offload 显形。
