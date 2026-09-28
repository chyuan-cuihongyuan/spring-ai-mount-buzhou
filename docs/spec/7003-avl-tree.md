# Spec 7003 — AvlTree AVL 树（effort #7003，U4）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7207–U7208，impl 2255）。
> 借鉴：Adelson-Velsky & Landis 1962 首个自平衡 BST。

## Problem Statement

有序映射的病：朴素 BST 顺序插入退化链 O(n)——**严格
平衡旋转保形面**缺失。

## Solution

`AvlTree`（core/concurrent，与 SplayTree/Treap 同包）：

- 每节点缓存子树高，插入/删除回溯重平衡（LL/RR 单旋、
  LR/RL 双旋），平衡因子恒 ∈ {−1,0,+1}——高度界
  1.44·log₂(n)；旋转由平衡因子唯一决定（同操作序列同构，
  无随机无惰性）；
- put upsert（同键覆值不增位）/get（缺席 null 诚实）/
  remove（缺席 fail-fast）/size/height/keysInOrder。

## User Stories

1. 作为索引作者，最坏 O(log n) 有序映射底座。
2. 作为审计作者，600 随机操作 vs TreeMap 圣像全等。

## Testing Decisions

- 600 随机操作（含缺席删 fail-fast）vs TreeMap 圣像逐步
  全等+键序全等；顺序/逆序各 200 插入高度 ≤11（朴素
  BST 最坏输入）；1000 随机 ≤15；upsert 覆值；fail-fast。

## Out of Scope

- 不做区间查询/范围视图；不做并发安全。

## Further Notes

- 与 SplayTree（6001）同族不同面：访问自调整摊还 vs
  严格平衡最坏；与 Treap（6002）不同面：随机优先级 vs
  确定性旋转。
- 里程碑：U4/50（8%）。
