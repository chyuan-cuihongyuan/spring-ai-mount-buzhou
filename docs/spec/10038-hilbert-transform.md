# Spec 10038 — HilbertTransform 解析信号包络（effort #10038，X39）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10077–X10078，impl 2441）。
> 借鉴：Hilbert 1912/SciPy hilbert 思想（HilbertCurve 已占异面：填充曲线）

## Problem Statement

窄带信号瞬时包络——实信号的解析信号模长面（FFT 单边化口径）。

## Solution

HilbertTransform（core/metrics）：envelope(double[])——FftIterative 正变换+频域核 h（直流/奈奎斯特点保 1、正频 ×2、负频置 0）+共轭-变换-共轭除 n 逆变换+逐点模长。

## Testing Decisions

整数周期正弦 A·sin 包络逐点 ≈A 手锚（1e-2）+双正弦复合包络恒幅面+直流信号包络 |c|+确定性+fail-fast 四面。

## Out of Scope

不做相位/瞬时频率读出面（消费方自组）；不做非幂长（FFT 域沿袭）；不做 IIR 近似包络（时域另立）。

## Further Notes

与 HilbertCurve（已占）异域异面：填充曲线 vs 解析信号；消费 FftIterative 流程内自组合第二例；Wave 7 信号与频谱族第三件。
