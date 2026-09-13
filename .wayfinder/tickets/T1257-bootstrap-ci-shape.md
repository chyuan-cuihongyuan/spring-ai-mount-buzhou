---
id: T1257
title: bootstrap 均值置信区间的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-13
---

## Question

I 会话第 4 轮：评估分数区间估计（Efron bootstrap percentile CI）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 4 轮 = effort #903 / spec 903 / impl 656）：缺口成立——`EvalScoreAnalytics.Report` 只有 min/max/mean 点估计，小数据集（本仓评估集常态 <100 项）上 mean 的抽样误差不可见，运维易把噪声当回归（spec 718 漂移基线的假警报源）。落点**扩 EvalScoreAnalytics**（同职责不炸类）：新增 `bootstrapMeanInterval(double[], confidenceLevel, resamples, seed)` 返回嵌套 record `MeanInterval(lower, upper, pointEstimate)`——Efron percentile bootstrap（可重复重采样→均值分布→α/2 分位截断）；seed 显式注入（可复现，spec 9 时钟注入同风格）；校验 samples 非空 / confidence ∈ (0,1) / resamples ≥ 1。不引第三方统计库（连 ThreadLocalRandom 显式种子即可）。
