# 903 — bootstrap 均值置信区间

> 来源：I 会话第 4 轮 = effort #903（[T1257](../../.wayfinder/tickets/T1257-bootstrap-ci-shape.md) / [T1258](../../.wayfinder/tickets/T1258-bootstrap-ci-verify.md) / impl 656）。借鉴：B. Efron [bootstrap](https://projecteuclid.org/journals/annals-of-statistics/volume-7/issue-1/Bootstrap-Methods--Another-Look-at-the-Jackknife/10.1214/aos/1176344552.full)（percentile interval）——小样本均值的抽样误差显形。

## Problem Statement

`EvalScoreAnalytics.Report` 只有 min/max/mean 点估计。本仓评估集常态 <100 项，mean 的抽样误差不可见——spec 718 漂移基线对 10 项数据集 ±0.1 的波动无法区分噪声与回归。bootstrap 重采样是零分布假设的区间估计标准法（不假设正态——评估分数常见双峰 pass/fail）。

## 目标

- `EvalScoreAnalytics` 新增 `bootstrapMeanInterval(double[] samples, double confidenceLevel, int resamples, long seed)`：
  - Efron percentile bootstrap：可重复重采样 `resamples` 次 → 每次算均值 → 经验分布 `[α/2, 1−α/2]` 分位点为区间；
  - 返回嵌套 record `MeanInterval(double lower, double upper, double pointEstimate)`；
  - `seed` 显式注入——同 seed 同结果（可复现，与依赖注入时钟 spec 9 同风格）；`RandomGenerator`（JDK 21）不引第三方；
- 校验：samples 非空、confidenceLevel ∈ 开区间 (0,1)、resamples ≥ 1（违者 IllegalArgumentException）；
- 纯函数不触 store（EvalFlakinessDetector 同型纪律）。

## 兼容性

纯增量：公共类新增静态方法 + 公共嵌套 record，零既有行为变化。
