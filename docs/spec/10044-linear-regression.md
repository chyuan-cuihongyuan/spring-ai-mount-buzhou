# Spec 10044 — LinearRegression OLS 正规方程（effort #10044，X45）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10089–X10090，impl 2447）。
> 借鉴：Gauss–Markov/NumPy lstsq 思想——scikit-learn LinearRegression 同源

## Problem Statement

最小二乘线性拟合——正规方程闭式解的基座面。

## Solution

LinearRegression（core/metrics）：fit(double[][] X, double[] y)——设计阵内联截距列（n×(d+1)）+正规方程 AᵀA w=Aᵀy 经 GaussianElimination.solve（core/concurrent，spec 10006）闭式解；返回 [截距,β1..βd]；奇异上浮 IllegalArgumentException。

## Testing Decisions

y=2x+1 精确复原 1e-9+双特征无共线面+带噪声最小二乘残差正交性（Aᵀ(y−Xw)≈0）+重复特征奇异 fail-fast+确定性+fail-fast 五面。

## Out of Scope

不做 Ridge/Lasso 正则面（另立）；不做 QR/SVD 数值稳定解法（条件数病态域不在承诺）；不做统计推断读数（R²/p 值另立）。

## Further Notes

消费 GaussianElimination（10006）流程间自组合第二例（TwoSat→Kosaraju 后）；Wave 8 经典机器学习族第三件。
