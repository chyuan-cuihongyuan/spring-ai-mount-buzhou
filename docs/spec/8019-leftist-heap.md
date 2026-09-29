# Spec 8019 — LeftistHeap 左偏可合并堆（effort #8019，V20）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8039–V8040，impl 2321）。
> 借鉴：Crane 1972 左偏树（FAU 论文——可合并堆经典）。

## Problem Statement

堆合并的病：二叉堆合并要全量重排 O(n)——**左偏树 O(log n)
合并（右路径交换）**，零路径左≥右保平衡。

## Solution

`LeftistHeap`（core/concurrent）：节点存零路径（npl）——
左孩子 npl ≥ 右孩子 npl，合并沿右路径递归交换；`merge`
O(log n)/`offer`=merge 单节点/`poll` 弹根（左右子合并）/
`peek` O(1)/`size` 读数；null 元素 fail-fast；空堆 poll
null 诚实缺省；确定性（同操作序同构——并列取先入）。

## Testing Decisions

- 手锚（两小堆合并后序+堆性质全量校验）；1000 随机操作
  vs PriorityQueue 圣像（poll 序全等+size 守恒）；npl
  不变量全量校验（左≥右）；fail-fast。

## Out of Scope

- 不做并发无锁变体；不做减键（索引面另件）。

## Further Notes

- 与 FibonacciHeap（8020）同族不同面：单次合并 O(log n)
  简单结构 vs 摊均 O(1) 合并的复杂结构。
- 里程碑：V20/50（40%）。
