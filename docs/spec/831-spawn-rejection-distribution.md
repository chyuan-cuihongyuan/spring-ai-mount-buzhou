# 831 — 会话准入拒绝分布

> 来源：H 会话第 32 轮 = effort #831 / [T1163](../../.wayfinder/tickets/T1163-spawn-rejection-distribution.md) / [T1164](../../.wayfinder/tickets/T1164-spawn-rejection-distribution-verify.md) / impl 584。
> 借鉴：k8s admission 拒绝读数（扩散轮）。

## Problem

SpawnGate 拒绝只发一次性事件：「这一小时被拒的会话都是什么原因」（floor 限流 vs drain vs timeout）无聚合表——容量规划缺依据。

## Solution

`SpawnRejectionDistribution`（core.backpressure，纯记账）：

- **原因分布**：record(reason, atMillis)——count+lastSeen(max)；键封顶 16（truncated 如实）。
- **报告**：计数降序+dominant（严格大于首达——平局稳定）+total。
- **喂点**：拒绝事件消费者装配侧——SpawnGate 零变更。

## 兼容性

纯新增；SpawnGate 零变更。

## 诚实边界

开集原因封顶丢弃如实；不解释原因语义；进程内存有界。
