# Spec 11002 — WalshHadamard 变换（effort #11002，Y3）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11005–Y11006，impl 2455）。
> 借鉴：Walsh 1923/Hadamard 1893 思想——量子计算/信号同源

## Problem Statement

±1 正交变换基座——O(n log n) 方波基展开面。

## Solution

WalshHadamard（core/metrics）：transform(double[]) 原位非归一——蝶形 (a+b,a−b) 宽 1 起逐层倍增（无三角函数全加减）；2 幂长 ≥2。

## Testing Decisions

单位脉冲全 1+常量仅直流手锚+对合性质 H(H(x))=n·x（随机信号圣像）+Parseval ΣX²=nΣx²+确定性+fail-fast 五面。

## Out of Scope

不做归一化面（H/√n 消费方自除）；不做序数排序面（sequency 排序另立）；不做多维面。

## Further Notes

±1 方波基正交变换——FFT 的方波镜像面；Wave 1 变换族第二件。
