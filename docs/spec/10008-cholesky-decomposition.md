# Spec 10008 — CholeskyDecomposition 分解（effort #10008，X9）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10017–X10018，impl 2411）。
> 借鉴：Cholesky 分解（Cholesky 1905——LAPACK potrf/NumPy cholesky 同源）

## Problem Statement

对称正定阵（协方差/正规方程/高斯过程）
用 LU 浪费一半算力且多余选主元——专用
平方根分解：A=LLᵀ，运算量减半、天然
稳定。

## Solution

CholeskyDecomposition（core/concurrent，
实例面）：构造期对角平方根+列缩放递推；
solve 双三角前代回代 O(n²)/次；det=
对角积平方；L 防御性拷贝读面；非对称/
非正定 fail-fast（主对角平方根域非正
契约明示）。

## Testing Decisions

LLᵀ 重构圣像（40 随机 SPD 阵逐元素）；
与 GaussianElimination 跨件互证（50 随
机系统全等）；行列式恒等 det(A)=（∏l）²
30 阵对拍；非对称/非正定/维数 fail-fast。

## Out of Scope

不做 LDLᵀ（免开方变体另立）；不做批量
多右端；不做条件数/逆阵面。

## Further Notes

与 LUDecomposition（10007）同域不同面：
对称正定专用 vs 通用方阵。
