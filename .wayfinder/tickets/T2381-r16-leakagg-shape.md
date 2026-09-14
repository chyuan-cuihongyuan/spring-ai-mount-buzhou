---
id: T2381
title: R16 泄漏疑似聚合接线的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2380
created: 2026-09-15
---

## Question

N 会话第 16 轮：聚合器接 listener 链——替换宿主 listener 还是复合？

## Resolution

选 **复合（双收）**。宿主 listener 是既有扩展缝（ObjectProvider 注入），替换会
吞宿主的消费面；复合让聚合器成为纯旁路（检测器行为零变更）。聚合器进程级单例
（Holder 模式——泄漏排行跨会话收敛）。
