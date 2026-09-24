# Spec 5048 — Pairing Heap 配对堆（effort #5048，S49）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6197–T6198，impl 2199）。
> 借鉴：Fredman-Sedgewick 配对堆（Haskell containers/Boost 同源思想）。

## Problem Statement

优先队列的病：二叉堆 meld O(n) 复制（可合并性缺失）或
二项堆结构不变量繁多（实现复杂）——**O(1) 可合并堆面**
缺失。

## Solution

`PairingHeap`（core/concurrent，long 键最小堆）：

- 多叉树最小堆；`insert`=与单节点堆 meld、`meld`=根比较
  挂钩 O(1)（所有权语义——他堆清空；self-meld fail-fast）；
- `extractMin` 后对孩子做**两趟合并**：先左到右两两配对、
  再右到左依次并回（摊还 O(log n)）；
- 读数：size/isEmpty；
- fail-fast：空堆取/弹、null meld、self-meld。

## User Stories

1. 作为调度作者，两堆合一 O(1)——合并式调度底座。
2. 作为审计作者，同操作序列同出序——结构确定性可回放。

## Testing Decisions

- 7 键脚本序弹出；300 键扰动插入（含重复）与 TreeMap
  圣像多重集全等（初版 meldNodes 递归覆盖兄弟链丢节点
  ——由该圣像钉住改对称非递归版）；meld 挂钩+所有权
  清空+四键有序弹出；双实例同操作同出序；空堆/self-meld
  fail-fast。

## Out of Scope

- 不做 decrease-key（配对堆裁剪面）；不做泛型比较器
 （long 键定构）；不做并发加锁。

## Further Notes

- 与 AgingPriorityQueue（exec）同族不同面：策略面（老化
  防饿死） vs 结构面（可合并堆）。Wave 9 独件。
- 里程碑：S49/50（98%）。
