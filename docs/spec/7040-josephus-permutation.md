# Spec 7040 — JosephusPermutation 约瑟夫环（effort #7040，U41）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7281–U7282，impl 2292）。
> 借鉴：Josephus 问题经典思想。

## Problem Statement

循环淘汰序的病：只算幸存者（出列序不可审计）——**
完整出列序模拟面**缺失。

## Solution

`JosephusPermutation`（core/metrics，静态工具面）：逐个
出列 O(nk) 朴素保留（出列序完整可审计）；k=1 退化顺序
出列；survivor 便捷面（与出列序末位一致）；确定性纯
函数；n,k≥1 越域 fail-fast。

## Testing Decisions

- n=5,k=2 / n=7,k=3 经典出列序逐值钉住；k=1 顺序退化；
  全覆盖不重不漏；幸存者一致；fail-fast。

## Out of Scope

- 不做 O(n) 递推公式面（模拟序完整优先）。

## Further Notes

- 与 FisherYatesShuffle（policy）同族不同面：随机洗牌 vs
  确定性计数出列。
- 里程碑：U41/50（82%）。
