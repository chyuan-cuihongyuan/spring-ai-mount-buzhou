# Spec 10010 — ConjugateGradient 共轭梯度（effort #10010，X11）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10021–X10022，impl 2413）。
> 借鉴：共轭梯度法（Hestenes–Stiefel 1952——SciPy cg/PETSc 同源）

## Problem Statement

大型 SPD 系统直接法 O(n³) 不可承受——
迭代法每步一次矩阵向量积 O(n²)（稀疏
更低），K 步理论收敛。

## Solution

ConjugateGradient（core/concurrent，静
态纯函数面）：solve(applyA,b,tol,maxIter)
——共轭方向+残差正交递推；接口
UnaryOperator<double[]> 矩阵向量积（稀
疏实现方自挂，稠密锚内置）；相对残差
收敛阈+实际迭代步读数 record；非正定
（pᵀAp≤0）fail-fast。

## Testing Decisions

40 随机 SPD 残差 <1e-6；良态 12 阶 ≤n
步收敛读数；与 GaussianElimination 跨
件互证；维数/容差/上限/非正定 fail-fast。

## Out of Scope

不做预条件子（PCG 另立）；不做稀疏存
储格式（CSR 已占异面）；不做非对称
（BiCG 另立）。

## Further Notes

Wave 2 数值线性代数族收束件：直接法
三件（Gauss/LU/QR）+分解专用一件
（Cholesky）+迭代一件（CG）全谱。
