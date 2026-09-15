# Spec 1836 — 库存周转读面（effort #1836，R37）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2873–T2874，impl 1437）。借鉴：
> 供应链库存周转（inventory turnover）——周转次数（消费/库存）量库存
> 活性，耗尽视界（库存/速率）量补货紧迫度。

## Problem Statement

Spill handle 存量只答「有多少」，答不了「多活/多紧」：周转贴地（死库存
——写完即死）与视界短（热门快耗）是两种相反的处方（TTL 激进 vs 预热），
缺一对互为倒数的读数。

## Solution

`TurnoverReadout`（buzhou-spill，静态纯函数）：

- `turns(stockUnits, consumedPerPeriod)` 周转次数（无库存 -1 哨兵；零消费
  0=死库存）；
- `depletionHorizonMillis(stockUnits, consumedPerMillis)` 耗尽视界（向上
  取整保守；零速率 -1 哨兵——无消费永不耗尽；零库存 0=已空）。

## User Stories

1. 作为 spill 治理者，周转 0.05/周期 → 死库存占绝对多数，TTL 该激进
  （SpillTieringAudit 的冷占比互证）。
2. 作为容量预警者，视界 250ms → 热门库存快见底，预热或扩容该启动。
3. 作为框架宿主，库存与消费口径（句柄数/字节数）自声明，纯读面零状态。

## Implementation Decisions

- 纯读面零状态；取整保守向上（宁可多等不可少报）。

## Testing Decisions

- 周转三档（3 轮/死库存/无库存哨兵）；视界三档（250ms/取整 253/零速率
  哨兵/零库存 0）；畸形五型 fail-fast。

## Out of Scope

- 不做自动 TTL/预热动作；不做周期聚合（归窗口族）。

## Further Notes

- 与 SpillTieringAudit 互补：那是三桶冷热分布，这是周转/视界速率面。
