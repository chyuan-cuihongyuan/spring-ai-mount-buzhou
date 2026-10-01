# Spec 11014 — RansacSampler 随机抽样一致（effort #11014，Y15）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11029–Y11030，impl 2467）。
> 借鉴：Fischler–Bolles 1981 思想——OpenCV 同源

## Problem Statement

污染观测的鲁棒模型拟合——共识最大化的随机采样面（最小二乘的外点病解互补）。

## Solution

RansacSampler（core/metrics）：fitLine(double[][],int,double,long)——每轮二点定线（x 差零退化跳过）、|y−(mx+b)|<threshold 计内点、最大共识线胜出；Line record(slope,intercept)+inliers 计数读面。

## Testing Decisions

20% 外点污染直线 y=2x+1 复原（斜率/截距 ±0.05）+全内点精确复原圣像+种子双面+fail-fast 四面。

## Out of Scope

不做通用模型泛型面（线拟合特化）；不做自适应迭代数（置信度公式另立）；不做加权 RANSAC 变体。

## Further Notes

与 LinearRegression（10044）同域不同面：共识鲁棒 vs 闭式最小二乘；Wave 3 稳健统计与校准族第二件。
