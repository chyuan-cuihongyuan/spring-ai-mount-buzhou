# Spec 5003 — 有界负载一致哈希（effort #5003，S4）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6107–T6108，impl 2154）。
> 借鉴：Consistent Hashing with Bounded Loads（Google vultr/vimeo 思想）。

## Problem Statement

一致哈希分片的病：热点节点过载（纯稳定性无上限——哈希
扎堆即雪崩）或全量重排（加节点全部 key 洗牌）——**稳定
映射 + 负载上限 + 确定性回退面**缺失。

## Solution

`BoundedLoadRing`（core/cache）：

- 节点注册：`addNode(id, capacity)`（容量=槽位数，≤0
  fail-fast；重复 id fail-fast）；
- `assign(key)`：稳定哈希定位起点（`hash(key) mod n`），满载
  则**线性探查**下一节点（确定性回退序），占一槽返回；
  总容量耗尽 ISE（诚实拒配）；
- 上限：单节点负载 ≤ 其 capacity——热点被结构性封顶；
- `release(node)` 释放一槽；`loadOf` 读数；
- 稳定性：节点集不变时同 key 同落点；节点增删仅影响探查序
  局部（线性探查变体的确定性落地口径，诚实入档）。

## User Stories

1. 作为分片路由作者，哈希扎堆不雪崩（上限封顶 + 确定性溢出）。
2. 作为审计作者，同 key 同节点同轨迹（确定性可回放）。

## Testing Decisions

- 稳定映射（不满载同 key 同点）；容量上限封顶（cap=1 溢出
  探查）；总容量耗尽 ISE；removeNode 后迁移确定；未知节点
  release/负容量 fail-fast。

## Out of Scope

- 不做虚拟节点倍增（物理槽位口径）；不做权重容量曲线
 （vimeo 加权版）；不做一致性迁移成本计量。

## Further Notes

- 与 RendezvousHashing（HRW 稳定选择）互补：纯稳定 vs
  有界回退。Wave 1 收口件（蓄水池/MinHash/指数滑窗均已占坑，
  本件自 Wave 2 前置顶补）。
- 里程碑：S4/50（8%）。
