---
id: T2617
title: 租约续期抖动读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

LeaseRenewalStats 的形状怎么裁决？（spec 1708 / effort #1708 / R9）（spec 1708 验收/裁决）

## Resolution

静态纯函数 analyze(intervalsMillis)→RenewalReport(samples/meanMillis/cv/maxSkewMillis)；cv=总体 std/mean 无量纲；负值忽略；n<2 哨兵 −1——etcd keepalive 节奏健康思想，先于租约丢失显形。
