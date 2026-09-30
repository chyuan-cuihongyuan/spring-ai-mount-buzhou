# Spec 9045 — TSP 2-opt 局部搜索（effort #9045，W46）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9091–W9092，impl 2398）。
> 借鉴：2-opt（Croes 1958——路由规划/物流配送同源的去交叉局部搜索）

## Problem Statement

初始序自交路径放大、全排列 O(n!) 不可行——
**2-opt**：边交换去交叉，单调逼近
（Euclidean 最优无交叉）。

## Solution

TspTwoOpt（core/policy，静态纯函数面）：
optimize(points,maxRounds)→Tour(order,
length)——first-improvement；lengthOf 校验面。

## Testing Decisions

单位正方形交叉序去交叉=4；两点/三点/
共线锚；25 随机不劣化+排列性；确定性；
fail-fast。

## Out of Scope

不做 3-opt/Or-opt（更深邻域另立）；
不做 Or 元启发混合；不做非欧度量
（交叉去不保证改进的域明示）。

## Further Notes

与 SimulatedAnnealing（9044）衔接；
与 GeometricMedian 同域不同面。
Wave 8 第四件。
