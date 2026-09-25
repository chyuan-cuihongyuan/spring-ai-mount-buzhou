# Spec 6002 — Treap 树堆（effort #6002，T3）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6205–T6206，impl 2203）。
> 借鉴：Seidel-Aragon 树堆论文思想（随机优先级平衡 BST；Lua/内核
> 随机化数据结构同源）。

## Problem Statement

有序映射的病：普通 BST 顺序插入退化链（O(n)），AVL/红黑旋转
不变量繁多（实现复杂）——**概率期望平衡 + 单旋转面**缺失。

## Solution

`Treap`（core/concurrent，long 键值映射）：

- BST 键序 + 最小堆优先级双不变量：新节点优先级由种子化
  SplitMix64 生成（同种子同结构——确定性可回放），插入后
  按优先级单旋上浮；
- remove 目标旋降至叶再摘除（子节点优先级小者上旋）；
- 读数：size/height（平衡度可见）；keysInOrder 中序全序；
- put upsert 覆盖不增位、get 缺席 null、remove 嫌席 fail-fast。

## User Stories

1. 作为索引作者，顺序键流插入仍得期望 O(log n)——无需
   复杂旋转不变量。
2. 作为审计作者，同种子同操作序列同结构——确定性可回放。

## Testing Decisions

- 500 键顺序插入（最坏 BST 输入）height≤32 + 中序全序 +
  TreeMap 圣像取值全等；扰动混合操作 500 步 oracle 全等；
  同种子双实例同构、异种子抽样根异；fail-fast。

## Out of Scope

- 不做泛型键（long 键定构）；不做并发加锁；不做 split/
  join 合并堆裁剪面。

## Further Notes

- 与 SplayTree（T2）同族不同面：访问自调整 vs 先验随机平衡；
  与 SkipList（S26）同族不同面：概率多层链 vs 随机优先级
  旋转树。
- 里程碑：T3/50（6%）。
