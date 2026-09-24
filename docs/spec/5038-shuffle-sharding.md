# Spec 5038 — Shuffle Sharding 洗牌分片（effort #5038，S39）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6177–T6178，impl 2189）。
> 借鉴：AWS shuffle sharding（租户×分片子集爆炸半径隔离思想）。

## Problem Statement

多租户路由的病：全租户共享全部分片（一个坏分片打全部
租户——爆炸半径 n/n）——**租户子集隔离面**缺失。

## Solution

`ShuffleSharding`（core/policy）：

- 每租户从 n 分片种子化随机挑 k 个：SplitMix64(seed XOR
  FNV-1a 64(租户指纹)) → Fisher-Yates 洗牌取前 k（升序
  确定性输出）；
- 请求只路由到本租户子集（`routesTo`）——坏分片对单租户
  爆炸半径 k/n；两租户子集期望重叠 k²/n（隔离但非互斥）；
- `overlap(a,b)` 共担面读数；同租户同子集（缓存分配）；
- fail-fast：shardCount<1、k 越界、null/空租户、分片
  下标越界。

## User Stories

1. 作为平台作者，坏分片只打 k/n 租户——爆炸半径有界。
2. 作为容量作者，overlap 读数——租户共担面可核算。

## Testing Decisions

- 圣像分配钉住（Python 64 位语义预演：tenant-a→[8,52]
  等 4 租户）；同种子同分配（跨实例全等）；routesTo 与
  assign 全域一致；200 租户两两平均重叠 <0.1（期望 0.04，
  实测 0.0398）+ 子集大小/去重；畸形 fail-fast。

## Out of Scope

- 不做分片负载上限（BoundedLoadRing 已覆盖）；不做租户
  迁移换子集（静态分配语义面）；不做真实请求路由器。

## Further Notes

- 与 BoundedLoadRing（spec 5003）同族不同面：子集隔离 vs
  全局负载有界。Wave 7 第三件。
- 里程碑：S39/50（78%）。
