# Spec 6026 — Indexed Heap 索引堆（effort #6026，T27）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6253–T6254，impl 2227）。
> 借鉴：Dijkstra/Prim 索引堆思想。源码于 T24 核账批预入档。

## Problem Statement

图算法就绪队列的病：朴素堆更新优先级需全扫 O(n)，PairingHeap
明示不做 decrease-key——**位置映射 O(1) 更新面**缺失。

## Solution

`IndexedHeap`（core/concurrent，源码已预载）：

- id→堆位哈希映射 O(1) 定位；updatePriority 双向（降级
  上浮/升级下沉各走对数）；同一 id 唯一在堆；
- 堆序确定性（优先级同按 id 字典序——同操作序列同出序）；
- fail-fast：重复 push、更新/查询缺席、空堆取弹。

## User Stories

1. 作为图算法作者，Dijkstra 松弛 O(log n)——就绪队列底座。
2. 作为审计作者，priorityOf/contains 显形——状态可查。

## Testing Decisions

- 300 随机 push 出序优先级不降；decrease-key 前插+increase
  后沉；混合操作序列终态逐值钉住；重复 id/缺席/空堆
  fail-fast。

## Out of Scope

- 不做合并堆（meld 是 PairingHeap 面）；不做泛型比较器
 （long 定构）。

## Further Notes

- 与 PairingHeap（5048）同族不同面：decrease-key 一等公民
  vs 可合并堆。
- 里程碑：T27/50（54%）。
