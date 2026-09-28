# Spec 7043 — LargestRectangleHistogram 柱状图最大矩形（effort #7043，U44）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7287–U7288，impl 2295）。
> 借鉴：单调栈 Largest Rectangle 经典思想。

## Problem Statement

柱状图最大矩形的病：全对边界枚举 O(n²)（柱数放大）
——**单调递增栈一次扫确定左右边界 O(n) 面**缺失。

## Solution

`LargestRectangleHistogram`（core/concurrent，静态工具面）：
每柱作高度时其左右第一个更矮柱由单调栈确定；哨兵 0 收
尾清栈（尾段不漏）；long 高度域；确定性纯函数；null/空
fail-fast。

## Testing Decisions

- 经典三例逐值；单柱/全等高/递增/递减退化；300 随机
  vs 暴力圣像；fail-fast。

## Out of Scope

- 不做二维全 1 矩形变体；不做最大正方形。

## Further Notes

- 与 MonotonicDeque 同族不同面：滑窗最值维护 vs 边界确
  定栈。
- 里程碑：U44/50（88%）。
