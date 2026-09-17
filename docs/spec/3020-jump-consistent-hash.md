# Spec 3020 — 跳增一致性哈希（effort #3020，R21）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5041–T5042，impl 2021）。
> 借鉴：Google jump consistent hash（Lamping & Masayuki 2014）。

## Problem Statement

分桶路由（会话分片/槽位归属）用朴素 key mod n——n 变（扩缩容）
即全量重排；一致性哈希环（Dynamo/Ketama）最小迁移但要虚节点表
O(n·v) 内存。键序均衡大、内存敏感、只尾端扩缩的场景缺一个零表
原语。

## Solution

`JumpConsistentHash`（core/policy，纯函数静态件）：

- `bucketOf(key, bucketCount)`：论文跳增——线性同余推进键，跳到
  j 的概率 1/(j+1)，终落桶 [0,n)；O(1) 空间零表零虚节点；
- 最小迁移：n→n+1 仅 ≈1/(n+1) 键移动且**全落新桶 n**（朴素取模
  全量重排病的根治）；
- `movesOnResize(key, old, new)` 迁移判定辅助；bucketCount ≥ 1
  fail-fast；负键可用（long 全域）。

## User Stories

1. 作为分片作者，万级槽位路由零内存——桶数尾端扩缩迁移最小。
2. 作为对账作者，同键同桶确定性——路由可复算可审计。

## Testing Decisions

- 万键值域夹持 [0,16)；10 万键 10 桶均匀性 ±0.01；10→11 扩容
  迁移率 1/11±0.01 且迁移键全落新桶（最小迁移的定量证据）；
  确定性（含负键/极值）；单桶恒 0；负键全域值域；0/负桶数
  fail-fast。

## Out of Scope

- 不支持任意桶增删/权重/异构容量（那是哈希环的领地——尾端扩缩
  是 jump 的契约边界，诚实声明）；不做 key 字符串预处理（哈希
  归 DeterministicHash）。

## Further Notes

- 与 ConsistentHashRing（2025）成对：环（任意增删+权重+虚节点
  内存）vs jump（零内存+尾端扩缩）——按场景选型。
- 里程碑：21/150。
