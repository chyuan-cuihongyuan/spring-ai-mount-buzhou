# Spec 4004 — KS 两样本检验（effort #4004，R5）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6009–T6010，impl 2105）。
> 借鉴：Kolmogorov 1933 / Smirnov 1948；scipy.stats.ks_2samp 同款。

## Problem Statement

A/B 两版回答质量分布、两模型延迟分布、两池错误率分布的「整体
错位」判定：t 检验只比均值（形状漂移盲区）、卡方要分桶（桶宽
主观）——**不假设形状、不分桶**的全分布对比一尺缺失。

## Solution

`KsTwoSample`（core/eval，纯静态）：

- D = sup|F₁(x) − F₂(x)| 双样本 ECDF 最大竖直距离（排序双指针扫）；
- p 值走渐近 Kolmogorov 分布 Q(λ)=2Σ(−1)^(k−1)e^(−2k²λ²)
  （Numerical Recipes 小样本校正 en=√ne+0.12+0.11/√ne；
  λ<0.4 区渐近式失效诚实返回 1——「无法拒绝」）；
- Result(statistic, pValue) 记录面；tie 同步双推进。

## User Stories

1. 作为评估作者，两模型延迟分布是否同源一尺可判——免分桶免均值。
2. 作为 A/B 作者，分布形状漂移（均值未动尾巴变长）不被漏检。

## Testing Decisions

- 全同样本 D=0 p=1；完全分离 D=1 p<0.001；半错位 D=0.5 p 居中
  (0.02,0.2)；奇偶交错双半 D<0.1 p>0.5；null/空样本两型 fail-fast。

## Out of Scope

- 不做单样本 KS（对理论分布）；不做 Anderson-Darling（尾部敏感
 * 加权变体）；不做精确小样本 p（查表法）。

## Further Notes

- 与 WilsonInterval（单比例）/ChiSquareUniformity（均匀性）成
  统计三尺；Wave 1（素描与统计族）收口。
- 里程碑：5/50。
