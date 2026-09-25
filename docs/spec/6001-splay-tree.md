# Spec 6001 — Splay Tree 伸展树（effort #6001，T2）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6203–T6204，impl 2202）。
> 借鉴：Sleator-Tarjan 伸展树论文思想（GCC splay-tree/链接切割树同源）。

## Problem Statement

有序映射的病：普通 BST 顺序插入退化成链（操作 O(n)），平衡树
（AVL/红黑）旋转不变量繁多（实现复杂）——**自调整无平衡元数据
面**缺失。

## Solution

`SplayTree`（core/concurrent，long 键值映射）：

- 访问即伸展：put/get/remove 后目标键自底/自顶旋至根（zig/
  zig-zag/zig-zig 三式）——热键恒在根附近（摊还 O(log n)）；
- put upsert（同键覆盖不增位）、get 缺席返回 null、remove
  嫌席 fail-fast；
- 读数：size/rootKey（结构确定性——最后访问键即根）；
- keysInOrder 中序全序导出。

## User Stories

1. 作为缓存作者，热键访问后自动上浮至根——无频率表的自调整热点。
2. 作为审计作者，同操作序列同根同中序——结构确定性可回放。

## Testing Decisions

- 500 键顺序插入（普通 BST 最坏输入）后中序与排序全等；扰动
  混合操作与 TreeMap 圣像全等；访问后 rootKey=被访键；删除
  后中序不变；双实例同操作同 rootKey 采样全等；缺席 remove
  /get null fail-fast。

## Out of Scope

- 不做泛型键（long 键定构）；不做并发加锁；不做分裂/合并
 （split/join 裁剪面）。

## Further Notes

- 与 TreeMap（JDK）不同面：自调整摊还界 vs 严格平衡界；
  与 PairingHeap（S49）同族不同面：优先序堆 vs 全序映射。
- 里程碑：T2/50（4%）。
