---
id: T2383
title: R17 工具失败负缓存的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2382
created: 2026-09-15
---

## Question

N 会话第 17 轮：失败记忆带不带「成功清除」机制？

## Resolution

选 **不带（纯 DNS 语义）**。测试暴露设计缺陷：TTL 内缓存命中短路返回错误文本，
「成功真调」永远不发生——成功清除不可达。恢复窗口 = TTL 本身（默认 30s 短窗
纪律兜底），语义诚实且实现最简。装饰器族（宿主 wrap，169 先例）。
