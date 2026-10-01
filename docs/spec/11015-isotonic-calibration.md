# Spec 11015 — IsotonicCalibration 保序回归（effort #11015，Y16）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11031–Y11032，impl 2468）。
> 借鉴：PAVA——scikit-learn IsotonicRegression 同源

## Problem Statement

非降约束的最小二乘投影——校准概率序的保序面。

## Solution

IsotonicCalibration（core/metrics）：fit(double[])——栈式 PAVA：逐值压块（sum,count），相邻块均值违序即合并直至无违序；块均值展开输出。

## Testing Decisions

[3,1,2] 手锚全 2 块+单调输入恒等面+递减输入全均值+总和守恒（随机 50 点圣像 1e-9）+非降性断言+确定性+fail-fast 五面。

## Out of Scope

不做加权面（权重 PAVA 另立）；不做插值消费面（阶跃/线性读出另立）；不做非降以外方向（递减由取反消费方自组）。

## Further Notes

校准概率序的基座原语——PlattScaling（11016）的互补面；Wave 3 稳健统计与校准族第三件。
