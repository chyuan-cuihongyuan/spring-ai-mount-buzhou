# Spec 9028 — Jackknife Estimator 刀切估计（effort #9028，W29）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9057–W9058，impl 2381）。
> 借鉴：Jackknife（Quenouille 1949/Tukey 1958——偏差消减与标准误同源）

## Problem Statement

单次全样本估计无不确定性读数、bootstrap
有放回计算型——**刀切**：确定性 leave-
one-out，n 个子样本恰好一次。

## Solution

JackknifeEstimator（core/eval，静态纯函数面）：
estimate(sample,statistic)→Result(estimate,
bias,standardError)；统计量可插拔。

## Testing Decisions

均值刀切 bias=0 且 se=s/√n 解析不变量；
方差偏差消减方向圣像；确定性；fail-fast。

## Out of Scope

不做 bootstrap 对照（M 系已占）；不做
加权刀切；不做删除-d jackknife（d>1）。

## Further Notes

与 TrimmedMean/GrubbsOutlier 同域不同面；
与 PermutationTest（9027）互补。Wave 5 收束件。
