# Spec 6046 — VanEmdeBoas 有界宇宙树（effort #6046，T46）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6291–T6292，impl 2246）。
> 借鉴：van Emde Boas 1975 有界宇宙树思想。

## Problem Statement

有界小整数全域（会话 id、槽位号）高频后继查询的病：
有序结构 O(log n) 后继——**O(log log u) 递归分簇面**缺失。

## Solution

`VanEmdeBoas`（core/concurrent）：

- 宇宙 2^bits（bits∈[1,20]）按半位分簇递归 + summary
  摘要；min 只在节点镜像、max 每层冗余（CLRS 约定）；
- 簇与 summary 惰性创建（稀疏集不预支全域内存）；
- insert（集合语义重复幂等）/delete（缺席 fail-fast）/
  contains/successor（缺席 -1）/minimum/maximum（空集 -1
  诚实）/size/isEmpty/universeSize；越域与位宽越域 fail-fast。

## User Stories

1. 作为调度作者，小整数全域 O(log log u) 后继取下一个
   可用槽位。
2. 作为审计作者，400 随机操作与 TreeSet 圣像五面逐步
   全等——行为可证。

## Testing Decisions

- 种子化 400 随机操作 vs TreeSet 圣像（insert/delete/
  contains/successor/最值五面逐步全等）；2/4/1M 三档宇宙
  端到端；稀疏插入；fail-fast 四路。

## Out of Scope

- 不做 merge/split；不做并发安全；bits>20 超会话语义
  明确拒绝。

## Further Notes

- 与 IndexedHeap（6026）同族不同面：优先级队列
  decrease-key vs 有界宇宙后继查询。
- 里程碑：T46/50（92%）。
