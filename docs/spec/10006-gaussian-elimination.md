# Spec 10006 — GaussianElimination 高斯消元（effort #10006，X7）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10013–X10014，impl 2409）。
> 借鉴：Gaussian elimination（Gauss 1810——NumPy linalg.solve/LAPACK dgesv 同源）

## Problem Statement

线性方程组 Ax=b 的基准直接法——裸序
消元小主元放大舍入误差，需部分主元
选行换序保数值稳定。

## Solution

GaussianElimination（core/concurrent，静态
纯函数面）：solve（部分主元前向消元+
回代）+determinant（交换计负号，奇异容
差内返 0 契约）；方阵契约；奇异 solve
fail-fast 而非静默。

## Testing Decisions

2×2 手锚；100 随机可解系统（对角加宽
保非奇）残差 <1e-8；零主元换序鲁棒；
行列式手锚 −14 与奇异返 0；fail-fast
五面（null/非方/维数不配/奇异/非方 det）。

## Out of Scope

不做 LU 复用分解（LUDecomposition 下轮
异面）；不做迭代法（ConjugateGradient
下波异面）；不做病态条件数诊断面。

## Further Notes

Wave 2 数值线性代数族首件；与 LUDeco
mposition 不同面：一次性求解 vs 分解复用。
