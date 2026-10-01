# Spec 10036 — FftIterative 迭代快速傅里叶（effort #10036，X37）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10073–X10074，impl 2439）。
> 借鉴：Cooley–Tukey 1965 思想——SciPy fft/NumPy 同源

## Problem Statement

DFT O(n²) 直接法的加速面——位反转+蝶形的迭代基-2 频谱基座。

## Solution

FftIterative（core/metrics）：transform(double[] re, double[] im) 原位——位反转置换重排+自底向上蝶形（W=n/2 起逐层倍增，twiddle 直接三角计算）+magnitudes(double[]) 便捷面（虚部零填充后取模）。

## Testing Decisions

单位脉冲全频带幅 1 手锚+全 1 信号仅直流手锚+8 点随机实信号与 O(n²) 直接 DFT 逐仓交叉互证 1e-9+Parseval 能量守恒（Σx²=n·Σ|X|²/N² 口径）+线性性+确定性+fail-fast 四面。

## Out of Scope

不做混合基/Bluestein 非幂长（论文变体另立）；不做实 FFT 优化变体（复用面足用）；不做逆变换面（共轭重构消费方自组）。

## Further Notes

信号与频谱族首件——HilbertTransform（10038）的流程内自组合基座；Wave 7 信号与频谱族首件。
