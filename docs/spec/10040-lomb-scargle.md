# Spec 10040 — LombScargle 不均匀采样频谱（effort #10040，X41）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10081–X10082，impl 2443）。
> 借鉴：Lomb 1976/Scargle 1982 思想——astro-Timeseries 同源

## Problem Statement

FFT 均匀采样前提不满足——不规则观测时刻的频谱估计面。

## Solution

LombScargle（core/metrics）：periodogram(double[] times, double[] values, double fMin, double fMax, int count)——每频点 ω=2πf、τ=atan2(Σsin 2ωt, Σcos 2ωt)/2ω、P=[(Σ(x−x̄)cos ω(t−τ))²/Σcos²+(Σ(x−x̄)sin ω(t−τ))²/Σsin²]/(2σ²)（σ² 总体方差口径）。

## Testing Decisions

均匀正弦 f0=4 频谱峰位复原手锚+随机删 30% 观测（不均匀）仍复原+白噪扫频无伪峰（峰值<0.5）+确定性+fail-fast 五面。

## Out of Scope

不做 fast 化（Press–Rybicki extirpolation 另立）；不做误差棒权重面；不做谐波合成重构。

## Further Notes

变距天文观测/传感器漏采的频谱基座——FFT 域的补位面；Wave 7 信号与频谱族收束件。
