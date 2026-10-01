# Spec 11012 — YuleWalker 方程（effort #11012，Y13）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11025–Y11026，impl 2465）。
> 借鉴：Yule 1927/Walker 1931 思想——Statsmodels 同源

## Problem Statement

自协方差域的 AR 谱估计方程组——Yule–Walker 主方程解面。

## Solution

YuleWalker（core/metrics）：solve(double[])——r0>0 归一化 r_i/r0 消费 DurbinLevinson.solve 得 a；σ²=r0·E_norm=r0−Σa_j r_j（未归一域复原）；复用 ArModel record。

## Testing Decisions

AR(1) 手锚（r=[2,1.6]→a=0.8、σ²=0.72）+AR(2) 理论自协方差往返互证圣像（σ² 域换算）+确定性+fail-fast 四面。

## Out of Scope

不做偏自相关读面（PACF 另立）；不做谱密度输出（SDF 另立）；不做阶数选择。

## Further Notes

与 DurbinLevinson（11010）同域不同面：自协方差域含 r0 主方程 vs 归一化自相关域递推——库内互喂第二例；Wave 3 谱与语音族首件。
