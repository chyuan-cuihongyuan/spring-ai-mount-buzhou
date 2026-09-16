# Spec 2022 — 复制计数器（effort #2022，R23）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3145–T3146，impl 1573）。
> 借鉴：CRDT G/PN-Counter（Dynamo/Cassandra 计数器口径）——与 spec
> 2006 LWW 寄存器同族，补 CRDT 计数面。

## Problem Statement

多实例计同一量（全局请求数 / 事件数 / 用量），朴素做法各自加后求和
merge 用「相加」——重放/重复传输即双计（at-least-once 网络下必双计）；
central 计数器又引入单点与同步开销。

## Solution

`ReplicatedCounter`（core/concurrent，synchronized 小临界区）：

- per-writer 分量：`increment(writerId, delta)`（正计数负扣减——PN
  合一；同 writer 分量单调由调用方自律）；
- `value()` = Σ分量；`components()` 字典序稳定快照（merge 载体）；
- `merge(other)`：**逐分量取 max**（因果保序——at-least-once 重复
  传输不双计）；CRDT 三性质：幂等（重复 merge 不变）、交换（A∪B =
  B∪A）、结合（多方两两 merge 收敛同终态）。

## User Stories

1. 作为多实例计量作者，分量快照随意重传——不双计（幂等）。
2. 作为对账者，components() 分量面可逐 writer 核对。

## Testing Decisions

- 单 writer 累计；多 writer Σ；负 delta 扣减；merge 逐分量 max（含
  新 writer 并入）；幂等（重复 merge 同值）；交换（正反序同分量）；
  三方并发收敛；畸形三型 fail-fast。

## Out of Scope

- 不做正负分计数分离（PN 双 G-Counter 经典结构——合一分量口径）；
- 不接 metrics 桥（跨实例计数读面归后续轮）。

## Further Notes

- 与 LWW 寄存器（spec 2006）成对：值域定序（单值）vs 计数收敛
  （聚合量）。
