# Spec 5027 — Deficit Round Robin 亏空调度（effort #5027，S28）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6155–T6156，impl 2178）。
> 借鉴：Deficit Round Robin（Shreedhar-Varghese——亏空记账字节公平）。

## Problem Statement

异构负载公平排队的病：纯轮询按条数（大包小包同价——字节
不公平）或按字节全排序（长包饿死短包流）——**亏空记账面**
缺失。

## Solution

`DeficitRoundRobin`（core/policy）：

- `enqueue(queue, payloadId, sizeBytes)`：入队（队列字节
  容量满返回 false）；
- `dequeue()`：轮转各队——队亏空 `deficit += quantum`，队头
  size ≤ deficit 则发出并扣减；队空则亏空清零跳过；单轮
  无可服务返回 null（调用方稍后再试——确定性不阻塞）；
- 亏空跨轮结转——小队列积少成多不被大包压死；
- 读数：deficits/queueBytes；fail-fast：quantum≤0、未知队列、
  size≤0。

## User Stories

1. 作为多队列作者，字节维度公平且无流被饿死。
2. 作为审计作者，同入队序列同服务序（确定性可回放）。

## Testing Decisions

- quantum 记账（15>10 结转、次轮 20≥15 发出）；队空亏空
  清零；容量满拒；未知队列/size≤0 fail-fast；确定性回放。

## Out of Scope

- 不做分层 DRR（HDD）；不做 latency 保证（QoS 面）；
- 不做动态增删队列。

## Further Notes

- 与 WeightedRoundRobin（S27）同族不同面：权重平滑 vs 字节
  亏空。Wave 5 第四件。
- 里程碑：S28/50（56%）。
