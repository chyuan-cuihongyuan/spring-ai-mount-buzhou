# Spec 3026 — Box-Muller 高斯采样器（effort #3026，R27）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5053–T5054，impl 2027）。
> 借鉴：Box & Muller 1958 极坐标变换。

## Problem Statement

退避抖动建模/蒙特卡洛扰动/合成噪声需要高斯样本，而均匀源
（RandomGenerator.nextDouble）到高斯的变换缺一个显式、可对账的
公共件（JDK nextGaussian 黑盒无口径可审计）。

## Solution

`GaussianSampler`（core/policy，纯函数静态件）：

- `samplePair(rng)`：u₁∈(0,1]（1−nextDouble 免 log(0)）与 u₂
  极坐标变换——一次产**一对**独立标准正态
  （z₀=√(−2ln u₁)cos 2πu₂、z₁=sin 分量）；
- `sample(rng)` 单样本（无缓存口径——不藏状态，纯函数性优先）；
  `sample(mean, σ, rng)` 平移缩放（σ=0 退化常量）；
- RandomGenerator 注入确定性回放；负 σ/null fail-fast。

## User Stories

1. 作为韧性作者，高斯退避抖动（±kσ 截尾）有正态源。
2. 作为评估作者，蒙特卡洛扰动的噪声源口径可审计可回放。

## Testing Decisions

- 10 万样本经验均值/标准差 0±0.02 / 1±0.02；N(10,2²) 平移缩放
  10±0.05 / 2±0.05；5 万对两分量均值皆 0±0.03；1σ 覆盖 68.27%±1%
  与 2σ 95.45%±1%（分布形状贴理论）；σ=0 恒常量；同种子回放；
  负 σ/null 三路 fail-fast。

## Out of Scope

- 不做 Ziggurat（拒绝采样高速变体留白——本件变换式零拒绝）；
  不做截尾/相关性结构（Copula 留白）；不接退避策略（接线归
  调用方）。

## Further Notes

- 与 GumbelMax/Nucleus/WeightedSample 成采样族第四件：均匀→
  离散选择的三个近亲 + 均匀→连续高斯的本件。
- 里程碑：27/150。
