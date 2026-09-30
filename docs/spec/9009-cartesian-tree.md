# Spec 9009 — Cartesian Tree 笛卡尔树（effort #9009，W10）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9019–W9020，impl 2362）。
> 借鉴：Cartesian tree（Vuillemin 1980——CP-algorithms RMQ↔LCA 桥经典）

## Problem Statement

逐点插入建树的病：O(n log n) 起步且结构随插入
序漂移——**单调栈 O(n) 一次成型**：值堆序+下标中序
双不变量。

## Solution

CartesianTree（core/concurrent）：of(values) 建树；
root/parentOf/leftChildOf/rightChildOf/lca/rangeMinIndex
（RMQ↔LCA 桥）；并列值低下标定胜。

## Testing Decisions

{8,2,5,1,4} 手锚；升序右脊链；RMQ 桥 4 手锚+
60 随机区间 vs 暴力圣像；堆序/中序双不变量 30 随机
数组；确定性；fail-fast。

## Out of Scope

不做动态更新（静态面——Treap 管动态）；不做
加权 RMQ（int 值域明示）；不做持久化。

## Further Notes

与 Treap（6002）同构不同源；Wave 2 第四件。
