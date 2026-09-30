# Spec 9033 — Geometric Median 几何中位数（effort #9033，W34）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9067–W9068，impl 2386）。
> 借鉴：Weiszfeld 1937（Fermat-Weber 设施选址——GIS 选址/k-means 前身同源）

## Problem Statement

质心对离群敏感、坐标轴中位数非联合最优——
**几何中位数**：距离和最小的 L1 联合
最优（无解析解，迭代逼近）。

## Solution

GeometricMedian（core/policy，静态纯函数面）：
median(points,iterations)——Weiszfeld 迭代；
totalDistance 目标读数；ε 防除。

## Testing Decisions

对称双点/等边三角/单点锚；离群稳健
圣像（距离和<质心+贴近簇）；迭代单调改进；
确定性；fail-fast。

## Out of Scope

不做容忍度收敛承诺（步数明示）；不做
加权变体；不做多中位数（k-median 面）。

## Further Notes

与 KdTree/ClosestPair 同域不同面；
与 KMeansClustering（eval）同根。
Wave 6 第四件。
