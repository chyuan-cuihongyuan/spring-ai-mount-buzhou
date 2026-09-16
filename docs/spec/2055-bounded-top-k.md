# Spec 2055 — 有界 Top-K 收集器（effort #2055，R56）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3211–T3212，impl 1606）。
> 借鉴：流式 top-K 小顶堆——守门员逐换 O(log K)。

## Problem Statement

流式榜单（慢调用榜 / 热点榜 / 大额账单榜）：存全集再排序内存 O(n)
不可持续；ToolSlowLog 的 FIFO 榜按时间窗留尾——「按值留大」的口径
缺件。

## Solution

`BoundedTopK<T>`（core/metrics，synchronized 小临界区）：

- `offer(score, item)`：未满即入；满则与**守门员**（堆顶=现任第 K
  名）比——严格大于即逐守入门（evicted 计数），否则落选（rejected
  计数）；同分守门员保位（先入者优先）；
- `top()` 降序快照；`gatekeeperScore()`（空榜 −∞——入榜门槛读数）；
- O(log K) 每推入 / O(K) 内存恒定；契约：capacity ≥ 1、item 非 null、
  score 非 NaN fail-fast。

## User Stories

1. 作为榜单作者，万级流只留前 K——内存恒定，守门员分数即入榜门槛
   实时可读。
2. 作为对账者，evicted/rejected 双计数——榜单换防频率与落选量显形。

## Testing Decisions

- 降序前三（9/7/5）落选 1；胜守门员逐换 evicted 1；同分保位（先入
  者留）；未满全收门槛=最小；空榜 −∞；万级递增流恰留 9995–9999 且
  rejected=0/evicted=9995（递增全逐换）；畸形三型 fail-fast。

## Out of Scope

- 不做时间窗衰减（与 FIFO 榜组合归调用方）；不接 ToolSlowLog（接线
  归后续轮）。

## Further Notes

- 与 ToolSlowLog 互补：时间窗留尾 vs 按值留大。
