# Spec 11003 — HaarWavelet 小波（effort #11003，Y4）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11007–Y11008，impl 2456）。
> 借鉴：Haar 1909 思想——pywt/scipy 同源

## Problem Statement

多分辨分析最简正交小波——均值/差值的逐层折半分解面。

## Solution

HaarWavelet（core/metrics）：forward(double[])——每层对相邻对 (x₀+x₁)/√2、(x₀−x₁)/√2 得近似/细节，近似段递归至单点；输出 [最粗近似, 细节由粗到细] 树状布局；正交归一（÷√2）。

## Testing Decisions

[1,2,3,4] 手锚（[5,−2,−1/√2,−1/√2]）+常量信号细节全零+Parseval 能量守恒（随机 8 点圣像 1e-9）+确定性+fail-fast 五面。

## Out of Scope

不做逆变换面（重构消费方另立）；不做 Daubechies 族（Y7 另立）；不做二维分解。

## Further Notes

最简正交小波——多分辨分析锚定件；Wave 1 变换族第三件。
