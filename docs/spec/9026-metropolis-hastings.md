# Spec 9026 — Metropolis-Hastings MCMC 采样（effort #9026，W27）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9053–W9054，impl 2379）。
> 借鉴：Metropolis 1953/Hastings 1970（物理系综/贝叶斯后验采样同源——Stan/PyMC 底层思想）

## Problem Statement

逆 CDF 需解析形式、拒绝采样需包络常数——
**MH**：只需未归一化密度可算，密度比
接受检验构造平稳分布。

## Solution

MetropolisHastings（core/experiment，静态纯
函数面）：sample(logDensity,start,step,draws,
burnIn,random)；log 域防下溢。

## Testing Decisions

标准正负矩圣像；双峰两模覆盖；同种子
逐值全等；fail-fast 四面。

## Out of Scope

不做多维/哈密顿（HMC/NUTS 面）；不做
自适应步长（burn-in 调参面）；不做提案
不对称一般形（Hastings 比率面）。

## Further Notes

与 RejectionSampler 同域不同面。
Wave 5 第三件。
