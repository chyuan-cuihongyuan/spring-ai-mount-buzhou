# Spec 7025 — PatienceLis 耐心 LIS（effort #7025，U26）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7251–U7252，impl 2277）。
> 借鉴：耐心排序（Patience game）思想。

## Problem Statement

最长递增子序列的病：DP O(n²)（长序列放大）——**二分
找堆 O(n log n) + topIndex 父链回溯面**缺失。

## Solution

`PatienceLis`（core/metrics，静态工具面）：堆顶可放即放
否则新开一堆，堆数=LIS 长；严格递增语义（相等不续
——明示）；返回长度+topIndex 父链回溯的 LIS 实例
（canonical 确定）；long 域；null fail-fast（空列长 0）。

## Testing Decisions

- 手锚（10,9,2,5,3,7,101,18 长 4——实例不钉死，合法性
  由性质钉）；300 随机 vs O(n²) DP 圣像+递增/子序列
  性质；fail-fast。

## Out of Scope

- 不做最长非降变体；不做计数 LIS 条数。

## Further Notes

- 与 MonotonicDeque（同包）同族不同面：滑窗最值 vs 全序
  列 LIS。
- 里程碑：U26/50（52%）。
