# Spec 5032 — B+ Tree 有序索引（effort #5032，S33）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6165–T6166，impl 2183）。
> 借鉴：MySQL InnoDB/数据库存储引擎 B+ 树（叶层全键值+内节点分隔）。

## Problem Statement

有序索引的病：有序数组插入 O(n)（搬移放大）或平衡二叉树
树高 O(log2 n)（扇出小、层多）——**高扇出确定性平衡面**
缺失。

## Solution

`BPlusTree`（core/metrics）：

- 全部键值落叶层、内节点只放分隔键（扇出=m——树高即
  比较/寻址次数上界）；叶层 next 链顺序扫描（范围扫底座）；
- put upsert（同键覆盖不增位）；满节点确定性对半分裂：
  叶分裂分隔键=左半末键**复制**上提（等键走左子树——
  childIndexOf 严格小于计数），内节点分裂中位分隔键
  **移动**上提（不保留在任一半）；根分裂树高 +1；
- 读数：size/height/entriesInOrder（叶链全序）、
  containsKey/getOrDefault（无 null 面）；
- fail-fast：maxKeys < 2。

## User Stories

1. 作为索引作者，扇出 m 树高 O(log_m n)——每查询层少。
2. 作为范围扫作者，叶链顺序遍历无需回溯中序栈。

## Testing Decisions

- 顺序 64 键全序可查（含缺席键）；upsert 覆盖不增位；
  扰动 500 次插入（(i×37)%97 必然大量重复+分裂）与
  TreeMap 圣像键序全等钉住（等键查找回归测试——childIndexOf
  严格小于计数修复面）；树高单叶=1、64 键 order=4 ≥2 且
  ≤5；maxKeys<2 fail-fast。

## Out of Scope

- 不做删除（借用/合并面不在本件）；不做磁盘页/缓冲池
 （本件是结构语义面）；不做泛型键（int 键定构，字符串
  键路由由 RadixTree 覆盖）。

## Further Notes

- 与 SkipList（spec 5025）同族不同面：概率多层链 vs 确定性
  多路平衡树。Wave 6 第三件。
- 里程碑：S33/50（66%）。
