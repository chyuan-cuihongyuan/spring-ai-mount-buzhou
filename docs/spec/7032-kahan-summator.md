# Spec 7032 — KahanSummator 补偿求和（effort #7032，U33）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7265–U7266，impl 2284）。
> 借鉴：Kahan 1965 补偿求和思想。

## Problem Statement

浮点连加的病：朴素累加误差随 n 线性放大（大数吃小数）
——**低位移存被舍入位下一轮补回面**缺失。

## Solution

`KahanSummator`（core/metrics）：每步 adjusted=value−comp
回收舍入位；误差 O(1) 独立于 n；确定性每步可复现；
double 域明示（不假装任意精度）；count/value 读数。

## Testing Decisions

- 大数吃小数经典（1e16 后接五个 1.0——朴素吞净、补偿
  救回部分，> 严格断言）；随机双实例确定性；空求和器。

## Out of Scope

- 不做 Neumaier 变体；不做成对求和。

## Further Notes

- 与 WelfordAccumulator 同族不同面：方差稳定累计 vs 求和
  补偿。
- 里程碑：U33/50（66%）。
