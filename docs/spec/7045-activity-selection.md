# Spec 7045 — ActivitySelectionGreedy 活动选择（effort #7045，U46）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7291–U7292，impl 2297）。
> 借鉴：最早结束时间贪心思想（算法导论经典，会议室排期同源）。

## Problem Statement

最大兼容子集的病：全枚举子集 O(2^n)——**按结束时间
贪心+交换论证最优性面**缺失。

## Solution

`ActivitySelectionGreedy`（core/policy，静态工具面）：按
结束时间升序能兼容即选（交换论证可证最优）；并列结束
按开始/编号 canonical（确定性）；[start,end) 半开语义
（start≥end fail-fast）；select/maxCount 双面。

## Testing Decisions

- 经典手锚逐值（含触界兼容 [0,5],[5,10]）；300 随机
  vs O(n²) DP 圣像+兼容性质；fail-fast 四路。

## Out of Scope

- 不做加权变体（DP 面）；不做多会议室。

## Further Notes

- 与 IntervalTree（metrics）同族不同面：重叠查询结构 vs
  最大兼容子集选择。
- 里程碑：U46/50（92%）。
