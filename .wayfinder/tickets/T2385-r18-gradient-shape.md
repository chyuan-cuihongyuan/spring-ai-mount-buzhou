---
id: T2385
title: R18 梯度式自适应并发的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2384
created: 2026-09-15
---

## Question

N 会话第 18 轮：延迟驱动限流——替换 AdaptiveBulkhead 还是正交新增？

## Resolution

选 **正交新增**。失败驱动 AIMD（spec 145）与延迟梯度是两个信号面：前者对
硬故障反应快、后者对软过载前兆敏感——替换丢语义，并存让宿主按负载特征选型。
调整策略钉住混合式：加性升（保守探测）/乘性降（果断规避）+ 容错带防抖 +
warmup 学习期——上升保守、下降果断与 Netflix 实现同工程取向。
