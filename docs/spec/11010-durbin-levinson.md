# Spec 11010 — DurbinLevinson AR 递推（effort #11010，Y11）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11021–Y11022，impl 2463）。
> 借鉴：Levinson 1947/Durbin 1960 思想——Statsmodels 同源

## Problem Statement

Toeplitz 线性系统的 O(p²) 递推解——AR 建模的谱分解基座。

## Solution

DurbinLevinson（core/metrics）：solve(double[])——E(0)=1 起，k=1..p：反射系数 k_k=(r_k−Σ a_j r_{k−j})/E_{k−1}、a 更新对称镜像+a_k=k_k、E_k=E_{k−1}(1−k_k²)；ArModel(coefficients,errorVariance)。

## Testing Decisions

AR(1) 闭式手锚（a=r1、E=1−r1²）+AR(2) 理论 ACF（r1=a1/(1−a2)、r2=a1r1+a2）逆变换往返互证圣像 1e-9+确定性+fail-fast 四面。

## Out of Scope

不做协方差法/格型滤波面；不做反射系数输出（内部量）；不做 AR 阶数选择（AIC 另立）。

## Further Notes

YuleWalker（11012）的前置基座——流程间自组合预留消费面；Wave 2 谱与语音族第四件。
