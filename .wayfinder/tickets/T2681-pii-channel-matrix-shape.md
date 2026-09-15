---
id: T2681
title: PII 通道×类型命中矩阵的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

PiiChannelMatrix 的形状怎么裁决？（spec 1740 / effort #1740 / R41）（spec 1740 验收/裁决）

## Resolution

Channel 三闭集 INPUT/STREAM/EXPORT×类型键（基数 32 超出并 _overflow_）二维计数+census 展平降序（unmodifiableMap 保序）+total——WAF 命中地图，「哪条路漏什么」一表定位。
