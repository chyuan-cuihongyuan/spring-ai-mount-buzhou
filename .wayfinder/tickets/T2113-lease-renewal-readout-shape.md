---
id: T2113
title: 租约续期健康读面（SessionLeaseGuard 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 7 轮（换题轮）：watchdog 续期健康面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：原题事件序列缺口无序号字段（不伪实现）；TTFT 已有（spec 46）——顺延租约看门狗轴（renewalCount 之外零读面）。续期循环已在 SessionLeaseGuard（TTL/3，Redisson watchdog 同型）。

形状裁决：guard 增量记账——failures（两失败路径终态前恰一次）+ minRemainingAtRenewalMillis 水位（续期时剩余租期最小值，饿死/抖动收窄信号）+ lastRenewalAt 时刻 + renewalStats() 嵌套 RenewalStats（-1/0 哨兵）；recordRenewalSuccess 提取公共记账消除两处重复；语义逐位不变，internal 包不入快照面。

Out of scope：runtime 级跨会话聚合；续期时延分布；节奏配置审计。
