# Spec 10037 — DctType2 正交 DCT-II 变换（effort #10037，X38）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10075–X10076，impl 2440）。
> 借鉴：Ahmed–Natarajan–Rao 1974 思想——JPEG/MP3 同源

## Problem Statement

频谱压缩变换面——JPEG 8 点块的能量压缩基座（正交归一口径）。

## Solution

DctType2（core/metrics）：transform(double[])——X[0]=sqrt(1/N)Σx、X[k]=sqrt(2/N)Σx·cos(π(2n+1)k/(2N))；正交归一（Parseval 严格守恒）；任意长度 ≥1 直接法。

## Testing Decisions

常数信号仅直流手锚（X[0]=c√N）+基向量正交归一（e_i 变换互点积 δ+范数 1）+Parseval 能量守恒+斜坡信号能量低频集中面+确定性+fail-fast 五面。

## Out of Scope

不做 DCT-III 逆变换面（消费方另立）；不做 FDCT 快速因子化（论文变体另立）；不做分块 2D 面（1D 原语）。

## Further Notes

JPEG 8 点块 1D 原语——压缩谱系的变换基座；Wave 7 信号与频谱族第二件。
