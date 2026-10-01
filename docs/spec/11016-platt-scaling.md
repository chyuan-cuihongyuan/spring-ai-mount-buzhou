# Spec 11016 — PlattScaling Platt 校准（effort #11016，Y17）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11033–Y11034，impl 2469）。
> 借鉴：Platt 1999 思想——LIBSVM/sklearn 同源

## Problem Statement

SVM 分数到概率的参数 sigmoid 校准面。

## Solution

PlattScaling（core/metrics）：fit(double[],int[],int)——Newton：w=p(1−p)、H=[[Σws²,Σws],[Σws,Σn]]、梯度 [Σ(p−y)s,Σ(p−y)]、阻尼 λ=1e-6 高斯消元步进；probability(score) 读面。

## Testing Decisions

已知 A=2、B=−1 种子生成 2000 样本复原（A±0.4、B±0.4）+完全可分 A>0 与序一致面+对称数据 B≈0+确定性+fail-fast 五面。

## Out of Scope

不做 Platt 目标平滑 y±（原论文目标平滑另立）；不做多类成对面；不做温度缩放（TC 变体另立）。

## Further Notes

参数化校准——IsotonicCalibration（11015）的非参数互补面；Wave 3 稳健统计与校准族收束件。
