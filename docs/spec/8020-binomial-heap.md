# Spec 8020 — BinomialHeap 二项堆（effort #8020，V21）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8041–V8042，impl 2322）。
> 借鉴：Vuillemin 1978 二项堆（可合并堆经典——勘误：原拟 FibonacciHeap
> 实现复杂度超时预算退回雾区——U24 同款纪律，换同族二项堆补位）。

## Problem Statement

可合并堆的病：二叉堆合并 O(n) 全量重排——**二项堆：度
互异的二项树森林，合并=二进制进位 O(log n)**。

## Solution

`BinomialHeap`（core/concurrent）：二项树森林（度 k 树
恰 2^k 节点，根最小）；`merge` 按度进位（同度两树链小根
为大根孩子）；`offer`=merge 单节点；`poll` 找最小根、摘下
后其孩子反序回并；`peek` O(log n) 扫根表（根表无全局指针
——明示）；`size` 读数；森林结构审计面（度互异+节点数
=Σ2^k）；空堆 poll null 诚实缺省；确定性（同操作序同
森林形态——并列取先入）。

## Testing Decisions

- 手锚（两小堆合并森林形态+度互异）；500 随机操作 vs
  PriorityQueue 圣像 poll 序全等+size 守恒；链式合并
  排水 vs 排序列表全等；结构审计全绿；fail-fast。

## Out of Scope

- 不做减键/删除任意元素；不做并发无锁变体。

## Further Notes

- 与 LeftistHeap（8019）同族不同面：单树 npl 右路径 vs
  森林二项树进位。
- 里程碑：V21/50（42%）。
