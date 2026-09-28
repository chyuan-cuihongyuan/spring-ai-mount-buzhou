# Spec 7044 — FloydCycleDetector 判圈（effort #7044，U45）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7289–U7290，impl 2296）。
> 借鉴：Floyd 1967 龟兔赛跑思想。

## Problem Statement

函数图判圈的病：HashSet 记访 O(n) 空间（大状态域不可
承受）——**快慢双指针 O(1) 空间判圈面**缺失。

## Solution

`FloydCycleDetector`（core/metrics）：快慢双指针相遇即
有环；环长（相遇点续走一圈）+入口（头部双指针二次迭
代）双读数；无环 −1 诚实缺省；函数图语义（每点恰一
后继）；越域 fail-fast；随机函数图 vs HashSet 圣像。

## Testing Decisions

- 手锚环（长 3 入口 2）；自环尾；随机函数图 vs HashSet
  圣像（长+入口）；fail-fast。

## Out of Scope

- 不做一般有向图（WaitForGraph 面）；不做 Brent 变体。

## Further Notes

- 与 WaitForGraph（6045）同族不同面：一般有向图增量环
  检测 vs 函数图 O(1) 空间判圈。
- 里程碑：U45/50（90%）。
