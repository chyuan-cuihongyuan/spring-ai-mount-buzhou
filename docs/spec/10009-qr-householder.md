# Spec 10009 — QrHouseholder 分解（effort #10009，X10）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10019–X10020，impl 2412）。
> 借鉴：Householder QR（Householder 1958——LAPACK geqrf/NumPy qr 同源）

## Problem Statement

超定方程组最小二乘——法方程 AᵀA
平方条件数放大误差——镜像反射 QR
数值稳定的标准直接法。

## Solution

QrHouseholder（core/concurrent，实例
面）：高瘦阵（m≥n）经济型分解——每
列构造 H=I−βvvᵀ 镜像反射一次清零对
角下元素，Q=H₁⋯Hₙ 逆序显式累积；
solve=Qᵀb 前乘+R 回代；秩亏（列范数
<容差）fail-fast。

## Testing Decisions

Q 列正交圣像（QᵀQ=I 逐元素 1e-8，
30 随机高瘦阵）；QR 重构全等；R 下三角
清零；最小二乘与法方程跨件互证（30
超定系统 1e-6）；横宽/行宽不齐/维数
fail-fast。

## Out of Scope

不做 Givens 旋转变体；不做列主元 QR
（秩 revealing 另立）；不做显式满 Q
（m×m——经济型即可）。

## Further Notes

开发勘误三处入档（arraycopy 行/列误
用+Q 初化只置首列+solve 取 n 误用
行数）；与 LU（10007）不同面：正交
变换 vs 三角分解。
