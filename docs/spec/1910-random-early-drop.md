# Spec 1910 — 随机早期丢弃（effort #1910，R111）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3021–T3022，impl 1511）。借鉴：
> RED（Random Early Detection，Sally Floyd/Van Jacobson 经典 AQM）——
> 队列逼近满位前按概率提前丢：minTh 以下不丢、maxTh 以上全丢、
> 两线之间线性概率——均匀丢而不是尾部突丢。

## Problem Statement

队列只在满时丢（尾丢）：丢的同时到达的成批请求一起死（同步突丢），
TCP 级重试又同时回来（全局同步）；早期概率丢把丢摊开——丢弃概率
曲线没有独立计算面。

## Solution

`RandomEarlyDrop`（core/backpressure，静态纯函数）：

- `dropProbability(queueSize, minTh, maxTh, maxP)`：minTh 以下 0；
  两线之间线性升到 maxP；maxTh 以上 maxP（RED 经典三段）；
- `verdict(queueSize, minTh, maxTh)`：ACCEPT（< minTh 或概率区未
  丢）——纯读数三态 COUNT/EARLY_DROP/TAIL_DROP……简化为双读数：
  `zone(...)` 返回 BELOW / LINEAR / ABOVE 三区。

## User Stories

1. 作为队列作者，minTh 50/maxTh 100/maxP 0.1：队列 75 → 丢率
   5%——提前摊丢而非尾部突丢。
2. 作为观测者，zone 读数接指标——LINEAR 区即预警区。
3. 作为公平性评审者，随机丢弃对突发流公平——同步性被打破。

## Implementation Decisions

- 纯函数零状态；minTh < maxTh、maxP ∈ (0,1]、队列数 ≥ 0
  fail-fast；概率区间线性不含突跳。

## Testing Decisions

- 三区各一例（25→0/75→0.05/120→0.1）；边界含下（恰 minTh 进入
  线性区/恰 maxTh 达 maxP）；畸形三型 fail-fast。

## Out of Scope

- 不做真实队列管理（归执行器）；不做 ECN 标记变体。

## Further Notes

- 与 负载脱落阶梯（R17 Envoy）互补：那是过载后按档脱落，这是
  过载前按概率摊丢。
