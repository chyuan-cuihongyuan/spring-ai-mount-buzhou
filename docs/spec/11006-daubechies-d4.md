# Spec 11006 — DaubechiesD4 小波（effort #11006，Y7）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11013–Y11014，impl 2459）。
> 借鉴：Daubechies 1988 思想——pywt 同源（HaarWavelet 已占异面：四系数紧支 vs 双系数）

## Problem Statement

紧支正交小波——光滑性与支撑宽度的四系数平衡面（Haar 的光滑升级）。

## Solution

DaubechiesD4（core/metrics）：forward(double[])——标准 D4 系数 c0..c3（(1±√3)/(4√2) 族）周期延拓滤波对 (近似 a[i]=c0x2i+c1x2i+1+c2x2i+2+c3x2i+3、细节 d[i]=c3x2i−c2x2i+1+c1x2i+2−c0x2i+3) 下采样折半至 <4。

## Testing Decisions

线性信号一层细节全零（双消失矩手锚）+常量信号细节全零+Parseval 能量守恒（随机 8 点圣像 1e-9）+4 点最短输入面+确定性+fail-fast 五面。

## Out of Scope

不做边界延拓变体（对称/镜像延拓另立）；不做逆变换重构面；不做更高阶 DN 族。

## Further Notes

与 HaarWavelet（11003）同域不同面：四系数紧支光滑 vs 双系数分段常值；Wave 2 变换族首件。
