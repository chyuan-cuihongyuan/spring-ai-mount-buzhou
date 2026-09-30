# Spec 10007 — LUDecomposition LU 分解（effort #10007，X8）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10015–X10016，impl 2410）。
> 借鉴：Doolittle LU（Doolittle 1924——LAPACK getrf/NumPy lu 同源）

## Problem Statement

同一矩阵多右端求解——高斯消元每次
O(n³) 重复消元——分解复用形态：一次
PA=LU，每次求解 O(n²)。

## Solution

LUDecomposition（core/concurrent，实例
面）：构造期部分主元分解（L 单位下三角
/U 上三角/perm 行序，行交换连带已算 L
元素）；solve 前代+回代+置换映射；
determinant=U 对角积；L/U/perm 防御性
拷贝读面。

## Testing Decisions

PA=LU 重构圣像（50 随机阵逐元素 1e-8）；
与 GaussianElimination 跨件互证（60 随
机系统全等）；行列式对拍 40 阵；奇异/
非方/维数 fail-fast。

## Out of Scope

不做 Cholesky（正定专用下轮异面）；不做
批量多右端批解面（单右端循环即可）；
不做条件数估计。

## Further Notes

与 GaussianElimination（10006）同域不同
面：分解复用 vs 一次性求解。
