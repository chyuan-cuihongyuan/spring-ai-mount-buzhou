# Spec 5043 — CRDT PN-Counter 正负计数器（effort #5043，S44）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6187–T6188，impl 2194）。
> 借鉴：Riak/Redis CRDT PN-Counter（G-Counter 对思想）。

## Problem Statement

分布式计数的病：中心序列化（协调开销+可用性损失）或
last-write-wins 覆盖（并发增量互相吞）——**无协调可
合并状态面**缺失。

## Solution

`CrdtPnCounter`（core/transaction）：

- P（增）/N（减）两个 G-Counter：每节点单调计数表；
  `increment/decrement(nodeId, amount≥0)` 本地单调记账，
  `value()`=P 和 − N 和（可负）；
- `merge`：按节点取 **max**（两侧）——交换律/幂等律/
  结合律三律成立，任意顺序同步副本必收敛；
- 读数：incrementEntries/decrementEntries（不可变副本
  审计面）；
- fail-fast：null/空 nodeId、负 amount、null other。

## User Stories

1. 作为多副本作者，各副本独立记账、同步即收敛——无协调。
2. 作为审计作者，单调表读数——每节点贡献可对账。

## Testing Decisions

- 增减取值（5+2−3=4）与单调表读数；纯减可负；CRDT 三律
  （交换：ab=ba 值+表全等；幂等：self-merge 不变；结合：
  (a⊕b)⊕c=a⊕(b⊕c)）；两副本乱序同步收敛（值 5+表全等）；
  fail-fast。

## Out of Scope

- 不做向量时钟因果判定（VectorClockOrder 已覆盖）；不做
  持久化副本存储；不做 OR-Set/多值寄存器（后续可扩族）。

## Further Notes

- 与 ReadRepair（spec 5018）互补：读路径修复 vs 无冲突
  可合并状态。Wave 8 第二件。
- 里程碑：S44/50（88%）。
