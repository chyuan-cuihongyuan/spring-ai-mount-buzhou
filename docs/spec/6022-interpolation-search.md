# Spec 6022 — Interpolation Search 插值查找（effort #6022，T23）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6245–T6246，impl 2223）。
> 借鉴：插值探测思想（数据库统计/数值库同源）。源码于 T18
> 对账批预入档。

## Problem Statement

均匀有序数组查找的病：对半二分无视分布信息（均匀域仍
O(log n) 探测）——**分布感知 O(log log n) 面**缺失。

## Solution

`InterpolationSearch`（core/concurrent，源码已预载）：

- pos = lo + (key−a[lo])·(hi−lo)/(a[hi]−a[lo]) 内插估位，
  double 先行减法防 long 溢出，[lo,hi] 钳制保证正确性
  不依赖分布（最坏退化 O(n) 有序扫——诚实边界）；
- 等值窗口零除守卫（a[lo]==a[hi] 直接判等）；
- fail-fast：null 数组。

## User Stories

1. 作为统计作者，均匀分位键近常数探测——大表查询底座。
2. 作为审计作者，找到即值等（重复值任一位次）——语义
   诚实。

## Testing Decisions

- 均匀 1000 数组全键命中+缺席 -1；50 随机数组×50 查询 vs
  Arrays.binarySearch 圣像（在位性+值等）；全等窗口终止；
  MIN/0/MAX 极值无溢出；fail-fast。

## Out of Scope

- 不做重复值位次承诺（任一命中）；不做浮点数组。

## Further Notes

- 与 Arrays.binarySearch 同族不同面：分布感知内插 vs 固定
  对半；与 SparseTable（T4）不同面：动态键探测 vs 静态
  区间聚合。
- 里程碑：T23/50（46%）。
