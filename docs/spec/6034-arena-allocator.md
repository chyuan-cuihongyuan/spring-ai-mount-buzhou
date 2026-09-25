# Spec 6034 — Arena Allocator 竞技场分配器（effort #6034，T35）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6269–T6270，impl 2235）。
> 借鉴：Netty/Flink pooled arena 思想。源码于 T30 核账批
> 预入档。

## Problem Statement

小对象高频分配的病：走通用堆（GC 压力放大）——**线性
bump+整池回收面**缺失。

## Solution

`ArenaAllocator`（core/memory，源码已预载）：

- 分配只前移水位指针（O(1) 无逐块元数据）；freeAll 一次
  归零；highWaterMark 历史峰值显形（审计——不被回收清零）；
- free 单块不支持（竞技场语义——显式抛出诚实边界）；
- fail-fast：capacity≤0、size≤0、越池、偏移越界。

## User Stories

1. 作为请求处理作者，每请求一池整池回收——GC 零压。
2. 作为容量作者，highWaterMark 显形——池容量规划依据。

## Testing Decisions

- 顺序分配偏移 0/10/30 确定性；freeAll 归零+峰值保留；
  回收后读零；越界/非法 size fail-fast。

## Out of Scope

- 不做单块释放（竞技场语义）；不做并发。

## Further Notes

- 与 BuddyAllocator（T31）同族不同面：线性 bump+整池回收
  vs 2 的幂分裂合并。
- 里程碑：T35/50（70%）。
