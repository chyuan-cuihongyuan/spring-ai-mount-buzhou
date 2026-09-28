# Spec 7013 — ClosestPair 最近点对（effort #7013，U14）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7227–U7228，impl 2265）。
> 借鉴：Bentley-Shamos 分治剪枝思想。

## Problem Statement

最近点对的病：全对暴力 O(n²)（万级点不可承受）——
**x 排序对半分治+中带有界比较面**缺失。

## Solution

`ClosestPair`（core/policy，静态工具面）：

- 分治 + 中带 y 序窗口比较（带内每点只比 y 距离平方小
  于当前最优者）——O(n log²n)（strip 逐层重排诚实变体，
  非镜像归并 O(n log n) 面——javadoc 明示）；
- 返回距离平方（long 域无浮点误差）；重合点 0（诚实）；
  坐标 ±10^9 安全域 fail-fast。

## User Stories

1. 作为地理作者，海量站点最近对一次求得。
2. 作为审计作者，随机点集 vs 暴力圣像逐步全等。

## Testing Decisions

- 手锚（含输入乱序）；重合点 0；200 随机点集（n≤150）
  vs 暴力 O(n²) 圣像全等；fail-fast 四路。

## Out of Scope

- 不做点对还原（只交距离面）；不做 k 近邻。

## Further Notes

- 与 KdTree（同包）同族不同面：全点集最近「对」vs 查询
  点最近「邻」。
- 里程碑：U14/50（28%）。
