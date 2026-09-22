# Spec 1909 — 闲谈收敛估算（effort #1909，R110）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3019–T3020，impl 1510）。借鉴：
> SWIM/hashicorp memberlist（万星级）gossip 传播语义——每节点每轮
> 向 f 个同伴闲谈，知情集合每轮 ×(1+f) 增长：N 节点全量知情需
> ⌈log_{f+1} N⌉ 轮——传播轮数与 fanout 配比事前可算。

## Problem Statement

成员变更传播的轮数拍脑袋：fanout 太小大集群变更传播以分钟计、
太大心跳流量挤占业务——「N 节点 fanout f 要几轮传遍、要在 R 轮
内传完 fanout 至少多少」缺反解面。

## Solution

`GossipConvergence`（core/concurrent，静态纯函数）：

- `roundsToConverge(nodes, fanout)`：⌈log_{f+1} N⌉——全量知情轮数；
- `informedAfter(nodes, fanout, rounds)`：min(N, (1+f)^rounds)——
  r 轮后知情节点数估计（封顶 N）；
- `fanoutFor(nodes, maxRounds)`：⌈N^{1/R} − 1⌉ 至少多少 fanout
  才能在 maxRounds 轮内传遍。

## User Stories

1. 作为成员协议作者，1024 节点 fanout 3 → 5 轮传遍——传播预算
   事前有数。
2. 作为流量治理者，要在 10 轮内传遍 1000 节点 → fanout ≥ 2——
   心跳流量上限直算。
3. 作为容量作者，三函数互逆可交叉验证。

## Implementation Decisions

- 指数增长模型（知情 ×(1+f)/轮，诚实近似——真实传播有重叠冗余，
  实际轮数 ≥ 估计）；nodes ≥ 1、fanout ≥ 1、rounds ≥ 0 fail-fast。

## Testing Decisions

- 经典 1024/3 → 5 轮；知情数封顶 N；反解 1000/10 → 2；N=1 零轮；
  畸形三型 fail-fast。

## Out of Scope

- 不做真实闲谈收发（归成员协议）；不做失效检测（归 Phi 面板）。

## Further Notes

- 与 PhiAccrualFailureDetector（单节点怀疑度）互补：那是检测谁
  死了，这是变更怎么传开。
